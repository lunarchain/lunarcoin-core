package lunar.vm

import lunar.vm.config.VMConfig
import lunar.vm.program.Program
import org.slf4j.LoggerFactory
import java.math.BigInteger

object VM {
    private val logger = LoggerFactory.getLogger("VM")
    private val dumpLogger = LoggerFactory.getLogger("dump")
    private val _32_ = BigInteger.valueOf(32)
    private val logString = "{}    Op: [{}]  Gas: [{}] Deep: [{}]  Hint: [{}]"

    private val MAX_MEM_SIZE = BigInteger.valueOf(Integer.MAX_VALUE.toLong())

    private var vmCounter = 0

    private var vmHook: VMHook? = null

    private val vmTrace: Boolean = false
    private val dumpBlock: Long = 0

    private fun calcMemGas(gasCosts: GasCost, oldMemSize: Long, newMemSize: BigInteger, copySize: Long): Long {
        var gasCost: Long = 0

        // Avoid overflows
        if (newMemSize.compareTo(MAX_MEM_SIZE) == 1) {
            throw Program.gasOverflow(newMemSize, MAX_MEM_SIZE)
        }

        // memory gas calc
        val memoryUsage = (newMemSize.toLong() + 31) / 32 * 32
        if (memoryUsage > oldMemSize) {
            val memWords = memoryUsage / 32
            val memWordsOld = oldMemSize / 32
            //TODO #POC9 c_quadCoeffDiv = 512, this should be a constant, not magic number
            val memGas = gasCosts.getMEMORY() * memWords + memWords * memWords / 512 - (gasCosts.getMEMORY() * memWordsOld + memWordsOld * memWordsOld / 512)
            gasCost += memGas
        }

        if (copySize > 0) {
            val copyGas = gasCosts.getCOPY_GAS() * ((copySize + 31) / 32)
            gasCost += copyGas
        }
        return gasCost
    }

    private fun isDeadAccount(program: Program, addr: ByteArray): Boolean {
        return !program.getStorage().isExist(addr) || program.getStorage().getAccountState(addr) != null
    }

    fun setVmHook(vmHook: VMHook) {
        VM.vmHook = vmHook
    }

    /**
     * Utility to calculate new total memory size needed for an operation.
     * <br></br> Basically just offset + size, unless size is 0, in which case the result is also 0.
     *
     * @param offset starting position of the memory
     * @param size number of bytes needed
     * @return offset + size, unless size is 0. In that case memNeeded is also 0.
     */
    private fun memNeeded(offset: DataWord, size: DataWord): BigInteger {
        return if (size.isZero()) BigInteger.ZERO else offset.value().add(size.value())
    }

    fun step(program: Program) {
        //TODO 此处省略对OPCODE版本的检查，如某些OPCODE是升级后出现需要与当前软件版本进行比对，后期加上
        val op = OpCode.code(program.getCurrentOp()) ?: throw Program.invalidOpCode(program.getCurrentOp())
        program.setLastOp(op.`val`())
        program.verifyStackSize(op.require())
        program.verifyStackOverflow(op.require(), op.ret()) //Check not exceeding stack limits
        val oldMemSize = program.getMemSize().toLong()
        val stack = program.getStack()
        var hint = ""
        val callGas: Long = 0
        val memWords: Long = 0 // parameters for logging
        var gasCost = op.getTier().asInt().toLong()
        val gasBefore = program.getGasLong()
        val stepBefore = program.getPC()
        //TODO 目前OPCODE的COST为HARDCODE
        val gasCosts = GasCost()
        var adjustedCallGas: DataWord? = null

        // Calculate fees and spend gas
        when(op) {

            OpCode.STOP -> {
                gasCost = gasCosts.getSTOP().toLong()
            }

            OpCode.SUICIDE -> {
                gasCost = gasCosts.getSUICIDE().toLong()
                val suicideAddressWord = stack[stack.size - 1]
                if (isDeadAccount(program, suicideAddressWord.getLast20Bytes()) && !program.getBalance(program.getOwnerAddress()).isZero()) {
                    gasCost += gasCosts.getNEW_ACCT_SUICIDE()
                }
            }

            OpCode.SSTORE -> {
                val newValue = stack[stack.size - 2]
                val oldValue = program.storageLoad(stack.peek())
                if (oldValue == null && !newValue.isZero())
                    gasCost = gasCosts.getSET_SSTORE().toLong()
                else if (oldValue != null && newValue.isZero()) {
                    // todo: GASREFUND counter policy

                    // refund step cost policy.
                    program.futureRefundGas(gasCosts.getREFUND_SSTORE().toLong())
                    gasCost = gasCosts.getCLEAR_SSTORE().toLong()
                } else
                    gasCost = gasCosts.getRESET_SSTORE().toLong()
            }

            OpCode.SLOAD -> {
                gasCost = gasCosts.getSLOAD().toLong()
            }

            OpCode.BALANCE -> {
                gasCost = gasCosts.getBALANCE().toLong()
            }

            OpCode.MSTORE -> {
                gasCost += calcMemGas(gasCosts, oldMemSize, memNeeded(stack.peek(), DataWord(32)), 0)
            }

            OpCode.MSTORE8 -> {
                gasCost += calcMemGas(gasCosts, oldMemSize, memNeeded(stack.peek(), DataWord(1)), 0)
            }
            OpCode.MLOAD -> {
                gasCost += calcMemGas(gasCosts, oldMemSize, memNeeded(stack.peek(), DataWord(32)), 0)
            }
            OpCode.RETURN, OpCode.REVERT -> {
                gasCost = gasCosts.getSTOP() + calcMemGas(gasCosts, oldMemSize,
                        memNeeded(stack.peek(), stack[stack.size - 2]), 0)
            }
            OpCode.SHA3 -> {
                gasCost = gasCosts.getSHA3() + calcMemGas(gasCosts, oldMemSize, memNeeded(stack.peek(), stack[stack.size - 2]), 0)
                val size = stack[stack.size - 2]
                val chunkUsed = (size.longValueSafe() + 31) / 32
                gasCost += chunkUsed * gasCosts.getSHA3_WORD()
            }
            OpCode.CALLDATACOPY, OpCode.RETURNDATACOPY -> {
                gasCost += calcMemGas(gasCosts, oldMemSize,
                        memNeeded(stack.peek(), stack[stack.size - 3]),
                        stack[stack.size - 3].longValueSafe())
            }
            OpCode.CODECOPY -> {
                gasCost += calcMemGas(gasCosts, oldMemSize,
                        memNeeded(stack.peek(), stack[stack.size - 3]),
                        stack[stack.size - 3].longValueSafe())
            }
            OpCode.EXTCODESIZE -> {
                gasCost = gasCosts.getEXT_CODE_SIZE().toLong()
            }
            OpCode.EXTCODECOPY -> {
                gasCost = gasCosts.getEXT_CODE_COPY() + calcMemGas(gasCosts, oldMemSize,
                        memNeeded(stack[stack.size - 2], stack[stack.size - 4]),
                        stack[stack.size - 4].longValueSafe())
            }
            OpCode.CALL, OpCode.CALLCODE, OpCode.DELEGATECALL, OpCode.STATICCALL -> {
                gasCost = gasCosts.getCALL().toLong()
                val callGasWord = stack[stack.size - 1]

                val callAddressWord = stack[stack.size - 2]

                val value = if (op.callHasValue())
                    stack[stack.size - 3]
                else
                    DataWord.ZERO

                //check to see if account does not exist and is not a precompiled contract
                if (op === OpCode.CALL) {
                    if (isDeadAccount(program, callAddressWord.getLast20Bytes()) && !value.isZero()) {
                        gasCost += gasCosts.getNEW_ACCT_CALL()
                    }
                }
                if (!value.isZero())
                    gasCost += gasCosts.getVT_CALL()

                val opOff = if (op.callHasValue()) 4 else 3
                val `in` = memNeeded(stack[stack.size - opOff], stack[stack.size - opOff - 1]) // in offset+size
                val out = memNeeded(stack[stack.size - opOff - 2], stack[stack.size - opOff - 3]) // out offset+size
                gasCost += calcMemGas(gasCosts, oldMemSize, `in`.max(out), 0)

                if (gasCost > program.getGas().longValueSafe()) {
                    throw Program.notEnoughOpGas(op, callGasWord, program.getGas())
                }

                val gasLeft = program.getGas().clone()
                gasLeft.sub(DataWord(gasCost))
                adjustedCallGas = VMConfig.getCallGas(op, callGasWord, gasLeft)
                gasCost += adjustedCallGas.longValueSafe()
            }

            OpCode.CREATE -> {
                gasCost = gasCosts.getCREATE() + calcMemGas(gasCosts, oldMemSize,
                        memNeeded(stack[stack.size - 2], stack[stack.size - 3]), 0)
            }
            OpCode.LOG0, OpCode.LOG1, OpCode.LOG2, OpCode.LOG3, OpCode.LOG4 -> {
                val nTopics = op.`val`() - OpCode.LOG0.`val`()

                val dataSize = stack[stack.size - 2].value()
                val dataCost = dataSize.multiply(BigInteger.valueOf(gasCosts.getLOG_DATA_GAS().toLong()))
                if (program.getGas().value() < dataCost) {
                    throw Program.notEnoughOpGas(op, dataCost, program.getGas().value())
                }

                gasCost = gasCosts.getLOG_GAS() +
                        gasCosts.getLOG_TOPIC_GAS() * nTopics +
                        gasCosts.getLOG_DATA_GAS() * stack[stack.size - 2].longValue() +
                        calcMemGas(gasCosts, oldMemSize, memNeeded(stack.peek(), stack[stack.size - 2]), 0)
            }
            OpCode.EXP -> {
                val exp = stack[stack.size - 2]
                val bytesOccupied = exp.bytesOccupied()
                gasCost = gasCosts.getEXP_GAS().toLong() + gasCosts.getEXP_BYTE_GAS().toLong() * bytesOccupied
            }

        }

        program.spendGas(gasCost, op.name)

        // Log debugging line for VM
        /*
        if (program.getNumber().intValue() === dumpBlock)
            this.dumpLine(op, gasBefore, gasCost + callGas, memWords, program)

        if (vmHook != null) {
            vmHook.step(program, op)
        }
        */
        // Execute operation
        when(op) {

        }

    }

    fun play(program: Program) {
        try {
            if (vmHook != null) {
                vmHook!!.startPlay(program)
            }

            if (program.byTestingSuite()) return

            while (!program.isStopped()) {
                step(program)
            }

        } catch (e: RuntimeException) {
            program.setRuntimeFailure(e)
        } catch (soe: StackOverflowError) {
            logger.error("\n !!! StackOverflowError: update your run command with -Xss2M !!!\n", soe)
            System.exit(-1)
        } finally {
            if (vmHook != null) {
                vmHook!!.stopPlay(program)
            }
        }
    }

}