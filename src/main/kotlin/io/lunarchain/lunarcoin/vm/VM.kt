package lunar.vm

import io.lunarchain.lunarcoin.util.ByteUtil.EMPTY_BYTE_ARRAY
import io.lunarchain.lunarcoin.util.CryptoUtil.Companion.sha3
import io.lunarchain.lunarcoin.vm.LogInfo
import io.lunarchain.lunarcoin.vm.MessageCall
import io.lunarchain.lunarcoin.vm.PrecompiledContracts
import lunar.vm.config.VMConfig
import lunar.vm.program.Program
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger
import java.util.ArrayList

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
            OpCode.STOP -> {
                program.setHReturn(EMPTY_BYTE_ARRAY)
                program.stop()
            }
            OpCode.ADD -> {
                val word1 = program.stackPop()
                val word2 = program.stackPop()

                if (logger.isInfoEnabled)
                    hint = word1.value().toString() + " + " + word2.value()

                word1.add(word2)
                program.stackPush(word1)
                program.step()
            }
            OpCode.MUL -> {
                val word1 = program.stackPop()
                val word2 = program.stackPop()

                if (logger.isInfoEnabled)
                    hint = word1.value().toString() + " * " + word2.value()

                word1.mul(word2)
                program.stackPush(word1)
                program.step()
            }

            OpCode.SUB -> {
                val word1 = program.stackPop()
                val word2 = program.stackPop()

                if (logger.isInfoEnabled)
                    hint = word1.value().toString() + " - " + word2.value()

                word1.sub(word2)
                program.stackPush(word1)
                program.step()
            }
            OpCode.DIV -> {
                val word1 = program.stackPop()
                val word2 = program.stackPop()

                if (logger.isInfoEnabled)
                    hint = word1.value().toString() + " / " + word2.value()

                word1.div(word2)
                program.stackPush(word1)
                program.step()
            }
            OpCode.SDIV -> {
                val word1 = program.stackPop()
                val word2 = program.stackPop()

                if (logger.isInfoEnabled)
                    hint = word1.sValue().toString() + " / " + word2.sValue()

                word1.sDiv(word2)
                program.stackPush(word1)
                program.step()
            }
            OpCode.MOD -> {
                val word1 = program.stackPop()
                val word2 = program.stackPop()

                if (logger.isInfoEnabled)
                    hint = word1.value().toString() + " % " + word2.value()

                word1.mod(word2)
                program.stackPush(word1)
                program.step()
            }
            OpCode.SMOD -> {
                val word1 = program.stackPop()
                val word2 = program.stackPop()

                if (logger.isInfoEnabled)
                    hint = word1.sValue().toString() + " #% " + word2.sValue()

                word1.sMod(word2)
                program.stackPush(word1)
                program.step()
            }
            OpCode.EXP -> {
                val word1 = program.stackPop()
                val word2 = program.stackPop()

                if (logger.isInfoEnabled)
                    hint = word1.value().toString() + " ** " + word2.value()

                word1.exp(word2)
                program.stackPush(word1)
                program.step()
            }
            OpCode.SIGNEXTEND -> {
                val word1 = program.stackPop()
                val k = word1.value()

                if (k.compareTo(_32_) < 0) {
                    val word2 = program.stackPop()
                    if (logger.isInfoEnabled)
                        hint = word1.toString() + "  " + word2.value()
                    word2.signExtend(k.toByte())
                    program.stackPush(word2)
                }
                program.step()
            }
            OpCode.NOT -> {
                val word1 = program.stackPop()
                word1.bnot()

                if (logger.isInfoEnabled)
                    hint = "" + word1.value()

                program.stackPush(word1)
                program.step()
            }
            OpCode.LT -> {
                // TODO: can be improved by not using BigInteger
                val word1 = program.stackPop()
                val word2 = program.stackPop()

                if (logger.isInfoEnabled)
                    hint = word1.value().toString() + " < " + word2.value()

                if (word1.value().compareTo(word2.value()) === -1) {
                    word1.and(DataWord.ZERO)
                    word1.getData()[31] = 1
                } else {
                    word1.and(DataWord.ZERO)
                }
                program.stackPush(word1)
                program.step()
            }
            OpCode.SLT -> {
                // TODO: can be improved by not using BigInteger
                val word1 = program.stackPop()
                val word2 = program.stackPop()

                if (logger.isInfoEnabled)
                    hint = word1.sValue().toString() + " < " + word2.sValue()

                if (word1.sValue().compareTo(word2.sValue()) === -1) {
                    word1.and(DataWord.ZERO)
                    word1.getData()[31] = 1
                } else {
                    word1.and(DataWord.ZERO)
                }
                program.stackPush(word1)
                program.step()
            }
            OpCode.SGT -> {
                // TODO: can be improved by not using BigInteger
                val word1 = program.stackPop()
                val word2 = program.stackPop()

                if (logger.isInfoEnabled)
                    hint = word1.sValue().toString() + " > " + word2.sValue()

                if (word1.sValue().compareTo(word2.sValue()) === 1) {
                    word1.and(DataWord.ZERO)
                    word1.getData()[31] = 1
                } else {
                    word1.and(DataWord.ZERO)
                }
                program.stackPush(word1)
                program.step()
            }

            OpCode.GT -> {
                // TODO: can be improved by not using BigInteger
                val word1 = program.stackPop()
                val word2 = program.stackPop()

                if (logger.isInfoEnabled)
                    hint = word1.value().toString() + " > " + word2.value()

                if (word1.value().compareTo(word2.value()) === 1) {
                    word1.and(DataWord.ZERO)
                    word1.getData()[31] = 1
                } else {
                    word1.and(DataWord.ZERO)
                }
                program.stackPush(word1)
                program.step()
            }
            OpCode.EQ -> {
                val word1 = program.stackPop()
                val word2 = program.stackPop()

                if (logger.isInfoEnabled)
                    hint = word1.value().toString() + " == " + word2.value()

                if (word1.xor(word2).isZero()) {
                    word1.and(DataWord.ZERO)
                    word1.getData()[31] = 1
                } else {
                    word1.and(DataWord.ZERO)
                }
                program.stackPush(word1)
                program.step()
            }

            OpCode.ISZERO -> {
                val word1 = program.stackPop()
                if (word1.isZero()) {
                    word1.getData()[31] = 1
                } else {
                    word1.and(DataWord.ZERO)
                }

                if (logger.isInfoEnabled)
                    hint = "" + word1.value()

                program.stackPush(word1)
                program.step()
            }

        /**
         * Bitwise Logic Operations
         */
            OpCode.AND -> {
                val word1 = program.stackPop()
                val word2 = program.stackPop()

                if (logger.isInfoEnabled)
                    hint = word1.value().toString() + " && " + word2.value()

                word1.and(word2)
                program.stackPush(word1)
                program.step()
            }
            OpCode.OR -> {
                val word1 = program.stackPop()
                val word2 = program.stackPop()

                if (logger.isInfoEnabled)
                    hint = word1.value().toString() + " || " + word2.value()

                word1.or(word2)
                program.stackPush(word1)
                program.step()
            }
            OpCode.XOR -> {
                val word1 = program.stackPop()
                val word2 = program.stackPop()

                if (logger.isInfoEnabled)
                    hint = word1.value().toString() + " ^ " + word2.value()

                word1.xor(word2)
                program.stackPush(word1)
                program.step()
            }

            OpCode.BYTE -> {
                val word1 = program.stackPop()
                val word2 = program.stackPop()
                val result: DataWord
                if (word1.value().compareTo(_32_) === -1) {
                    val tmp = word2.getData()[word1.intValue()]
                    word2.and(DataWord.ZERO)
                    word2.getData()[31] = tmp
                    result = word2
                } else {
                    result = DataWord()
                }

                if (logger.isInfoEnabled)
                    hint = "" + result.value()

                program.stackPush(result)
                program.step()
            }

            OpCode.ADDMOD -> {
                val word1 = program.stackPop()
                val word2 = program.stackPop()
                val word3 = program.stackPop()
                word1.addmod(word2, word3)
                program.stackPush(word1)
                program.step()
            }

            OpCode.MULMOD -> {
                val word1 = program.stackPop()
                val word2 = program.stackPop()
                val word3 = program.stackPop()
                word1.mulmod(word2, word3)
                program.stackPush(word1)
                program.step()
            }

        /**
         * SHA3
         */
            OpCode.SHA3 -> {
                val memOffsetData = program.stackPop()
                val lengthData = program.stackPop()
                val buffer = program.memoryChunk(memOffsetData.intValueSafe(), lengthData.intValueSafe())

                val encoded = sha3(buffer)
                val word = DataWord(encoded)

                if (logger.isInfoEnabled)
                    hint = word.toString()

                program.stackPush(word)
                program.step()
            }

        /**
         * Environmental Information
         */
            OpCode.ADDRESS -> {
                val address = program.getOwnerAddress()

                if (logger.isInfoEnabled)
                    hint = "address: " + Hex.toHexString(address.getLast20Bytes())

                program.stackPush(address)
                program.step()
            }

            OpCode.BALANCE -> {
                val address = program.stackPop()
                val balance = program.getBalance(address)

                if (logger.isInfoEnabled)
                    hint = ("address: "
                            + Hex.toHexString(address.getLast20Bytes())
                            + " balance: " + balance.toString())

                program.stackPush(balance)
                program.step()
            }

            OpCode.ORIGIN -> {
                val originAddress = program.getOriginAddress()

                if (logger.isInfoEnabled)
                    hint = "address: " + Hex.toHexString(originAddress.getLast20Bytes())

                program.stackPush(originAddress)
                program.step()
            }

            OpCode.CALLER -> {
                val callerAddress = program.getCallerAddress()

                if (logger.isInfoEnabled)
                    hint = "address: " + Hex.toHexString(callerAddress.getLast20Bytes())

                program.stackPush(callerAddress)
                program.step()
            }

            OpCode.CALLVALUE -> {
                val callValue = program.getCallValue()

                if (logger.isInfoEnabled)
                    hint = "value: " + callValue

                program.stackPush(callValue)
                program.step()
            }

            OpCode.CALLDATALOAD -> {
                val dataOffs = program.stackPop()
                val value = program.getDataValue(dataOffs)

                if (logger.isInfoEnabled)
                    hint = "data: " + value

                program.stackPush(value)
                program.step()
            }

            OpCode.CALLDATASIZE -> {
                val dataSize = program.getDataSize()

                if (logger.isInfoEnabled)
                    hint = "size: " + dataSize.value()

                program.stackPush(dataSize)
                program.step()
            }

            OpCode.CALLDATACOPY -> {
                val memOffsetData = program.stackPop()
                val dataOffsetData = program.stackPop()
                val lengthData = program.stackPop()

                val msgData = program.getDataCopy(dataOffsetData, lengthData)

                if (logger.isInfoEnabled)
                    hint = "data: " + Hex.toHexString(msgData)

                program.memorySave(memOffsetData.intValueSafe(), msgData)
                program.step()
            }

            OpCode.RETURNDATASIZE -> {
                val dataSize = program.getReturnDataBufferSize()

                if (logger.isInfoEnabled)
                    hint = "size: " + dataSize.value()

                program.stackPush(dataSize)
                program.step()
            }

            OpCode.RETURNDATACOPY -> {
                val memOffsetData = program.stackPop()
                val dataOffsetData = program.stackPop()
                val lengthData = program.stackPop()

                val msgData = program.getReturnDataBufferData(dataOffsetData, lengthData)
                        ?: throw Program.ReturnDataCopyIllegalBoundsException(dataOffsetData, lengthData, program.getReturnDataBufferSize().longValueSafe())

                if (logger.isInfoEnabled)
                    hint = "data: " + Hex.toHexString(msgData!!)

                program.memorySave(memOffsetData.intValueSafe(), msgData)
                program.step()
            }

            OpCode.CODESIZE, OpCode.EXTCODESIZE -> {

                val length: Int
                if (op === OpCode.CODESIZE)
                    length = program.getCode().size
                else {
                    val address = program.stackPop()
                    length = program.getCodeAt(address).size
                }
                val codeLength = DataWord(length)

                if (logger.isInfoEnabled)
                    hint = "size: " + length

                program.stackPush(codeLength)
                program.step()
            }

            OpCode.CODECOPY, OpCode.EXTCODECOPY -> {

                var fullCode = EMPTY_BYTE_ARRAY
                if (op === OpCode.CODECOPY)
                    fullCode = program.getCode()

                if (op === OpCode.EXTCODECOPY) {
                    val address = program.stackPop()
                    fullCode = program.getCodeAt(address)
                }

                val memOffset = program.stackPop().intValueSafe()
                val codeOffset = program.stackPop().intValueSafe()
                val lengthData = program.stackPop().intValueSafe()

                val sizeToBeCopied = if (codeOffset.toLong() + lengthData > fullCode.size)
                    if (fullCode.size < codeOffset) 0 else fullCode.size - codeOffset
                else
                    lengthData

                val codeCopy = ByteArray(lengthData)

                if (codeOffset < fullCode.size)
                    System.arraycopy(fullCode, codeOffset, codeCopy, 0, sizeToBeCopied)

                if (logger.isInfoEnabled)
                    hint = "code: " + Hex.toHexString(codeCopy)

                program.memorySave(memOffset, codeCopy)
                program.step()
            }

            OpCode.GASPRICE -> {
                val gasPrice = program.getGasPrice()

                if (logger.isInfoEnabled)
                    hint = "price: " + gasPrice.toString()

                program.stackPush(gasPrice)
                program.step()
            }
        /**
         * Block Information
         */
            OpCode.BLOCKHASH -> {

                val blockIndex = program.stackPop().intValueSafe()

                val blockHash = program.getBlockHash(blockIndex)

                if (logger.isInfoEnabled)
                    hint = "blockHash: " + blockHash

                program.stackPush(blockHash)
                program.step()
            }

            OpCode.COINBASE -> {
                val coinbase = program.getCoinbase()

                if (logger.isInfoEnabled)
                    hint = "coinbase: " + Hex.toHexString(coinbase.getLast20Bytes())

                program.stackPush(coinbase)
                program.step()
            }

            OpCode.TIMESTAMP -> {
                val timestamp = program.getTimestamp()

                if (logger.isInfoEnabled)
                    hint = "timestamp: " + timestamp.value()

                program.stackPush(timestamp)
                program.step()
            }

            OpCode.NUMBER -> {
                val number = program.getNumber()

                if (logger.isInfoEnabled)
                    hint = "number: " + number.value()

                program.stackPush(number)
                program.step()
            }

            OpCode.DIFFICULTY -> {
                val difficulty = program.getDifficulty()

                if (logger.isInfoEnabled)
                    hint = "difficulty: " + difficulty

                program.stackPush(difficulty)
                program.step()
            }

            OpCode.GASLIMIT -> {
                val gaslimit = program.getGasLimit()

                if (logger.isInfoEnabled)
                    hint = "gaslimit: " + gaslimit

                program.stackPush(gaslimit)
                program.step()
            }

            OpCode.POP -> {
                program.stackPop()
                program.step()
            }

            OpCode.DUP1, OpCode.DUP2, OpCode.DUP3, OpCode.DUP4,
            OpCode.DUP5, OpCode.DUP6, OpCode.DUP7, OpCode.DUP8,
            OpCode.DUP9, OpCode.DUP10, OpCode.DUP11, OpCode.DUP12,
            OpCode.DUP13, OpCode.DUP14, OpCode.DUP15, OpCode.DUP16 -> {

                val n = op.`val`() - OpCode.DUP1.`val`() + 1
                val word_1 = stack[stack.size - n]
                program.stackPush(word_1.clone())
                program.step()

            }
            OpCode.SWAP1, OpCode.SWAP2, OpCode.SWAP3, OpCode.SWAP4,
            OpCode.SWAP5, OpCode.SWAP6, OpCode.SWAP7, OpCode.SWAP8,
            OpCode.SWAP9, OpCode.SWAP10, OpCode.SWAP11, OpCode.SWAP12,
            OpCode.SWAP13, OpCode.SWAP14, OpCode.SWAP15, OpCode.SWAP16 -> {

                val n = op.`val`() - OpCode.SWAP1.`val`() + 2
                stack.swap(stack.size - 1, stack.size - n)
                program.step()
            }

            OpCode.LOG0,
            OpCode.LOG1,
            OpCode.LOG2,
            OpCode.LOG3,
            OpCode.LOG4 -> {

                if (program.isStaticCall()) throw Program.StaticCallModificationException()
                val address = program.getOwnerAddress()

                val memStart = stack.pop()
                val memOffset = stack.pop()

                val nTopics = op.`val`() - OpCode.LOG0.`val`()

                val topics: MutableList<DataWord> = ArrayList()
                for (i in 0 until nTopics) {
                    val topic = stack.pop()
                    topics.add(topic)
                }

                val data = program.memoryChunk(memStart.intValueSafe(), memOffset.intValueSafe())

                val logInfo = LogInfo(address.getLast20Bytes(), topics, data)

                if (logger.isInfoEnabled)
                    hint = logInfo.toString()

                program.getResult().addLogInfo(logInfo)
                program.step()
            }
            OpCode.MLOAD -> {
                val addr = program.stackPop()
                val data = program.memoryLoad(addr)

                if (logger.isInfoEnabled)
                    hint = "data: " + data

                program.stackPush(data)
                program.step()
            }

            OpCode.MSTORE -> {
                val addr = program.stackPop()
                val value = program.stackPop()

                if (logger.isInfoEnabled)
                    hint = "addr: $addr value: $value"

                program.memorySave(addr, value)
                program.step()
            }

            OpCode.MSTORE8 -> {
                val addr = program.stackPop()
                val value = program.stackPop()
                val byteVal = byteArrayOf(value.getData()[31])
                program.memorySave(addr.intValueSafe(), byteVal)
                program.step()
            }

            OpCode.SLOAD -> {
                val key = program.stackPop()
                var `val` = program.storageLoad(key)

                if (logger.isInfoEnabled)
                    hint = "key: $key value: $`val`"

                if (`val` == null)
                    `val` = key.and(DataWord.ZERO)

                program.stackPush(`val`)
                program.step()
            }

            OpCode.SSTORE -> {
                if (program.isStaticCall()) throw Program.StaticCallModificationException()

                val addr = program.stackPop()
                val value = program.stackPop()

                if (logger.isInfoEnabled)
                    hint = "[" + program.getOwnerAddress().toPrefixString() + "] key: " + addr + " value: " + value

                program.storageSave(addr, value)
                program.step()
            }

            OpCode.JUMP -> {
                val pos = program.stackPop()
                val nextPC = program.verifyJumpDest(pos)

                if (logger.isInfoEnabled)
                    hint = "~> " + nextPC

                program.setPC(nextPC)

            }

            OpCode.JUMPI -> {
                val pos = program.stackPop()
                val cond = program.stackPop()

                if (!cond.isZero()) {
                    val nextPC = program.verifyJumpDest(pos)

                    if (logger.isInfoEnabled)
                        hint = "~> " + nextPC

                    program.setPC(nextPC)
                } else {
                    program.step()
                }
            }

            OpCode.PC -> {
                val pc = program.getPC()
                val pcWord = DataWord(pc)

                if (logger.isInfoEnabled)
                    hint = pcWord.toString()

                program.stackPush(pcWord)
                program.step()
            }

            OpCode.MSIZE -> {
                val memSize = program.getMemSize()
                val wordMemSize = DataWord(memSize)

                if (logger.isInfoEnabled)
                    hint = "" + memSize

                program.stackPush(wordMemSize)
                program.step()
            }

            OpCode.GAS -> {
                val gas = program.getGas()

                if (logger.isInfoEnabled)
                    hint = "" + gas

                program.stackPush(gas)
                program.step()
            }


            OpCode.PUSH1,
            OpCode.PUSH2,
            OpCode.PUSH3,
            OpCode.PUSH4,
            OpCode.PUSH5,
            OpCode.PUSH6,
            OpCode.PUSH7,
            OpCode.PUSH8,
            OpCode.PUSH9,
            OpCode.PUSH10,
            OpCode.PUSH11,
            OpCode.PUSH12,
            OpCode.PUSH13,
            OpCode.PUSH14,
            OpCode.PUSH15,
            OpCode.PUSH16,
            OpCode.PUSH17,
            OpCode.PUSH18,
            OpCode.PUSH19,
            OpCode.PUSH20,
            OpCode.PUSH21,
            OpCode.PUSH22,
            OpCode.PUSH23,
            OpCode.PUSH24,
            OpCode.PUSH25,
            OpCode.PUSH26,
            OpCode.PUSH27,
            OpCode.PUSH28,
            OpCode.PUSH29,
            OpCode.PUSH30,
            OpCode.PUSH31,
            OpCode.PUSH32 -> {
                program.step()
                val nPush = op.`val`() - OpCode.PUSH1.`val`() + 1

                val data = program.sweep(nPush)

                if (logger.isInfoEnabled)
                    hint = "" + Hex.toHexString(data)

                program.stackPush(data)
            }

            OpCode.JUMPDEST -> { program.step() }
            OpCode.CREATE -> {
                if (program.isStaticCall()) throw Program.StaticCallModificationException()

                val value = program.stackPop()
                val inOffset = program.stackPop()
                val inSize = program.stackPop()

                if (logger.isInfoEnabled)
                    logger.info(logString, String.format("%5s", "[" + program.getPC() + "]"),
                            String.format("%-12s", op.name),
                            program.getGas().value(),
                            program.getCallDeep(), hint)

                program.createContract(value, inOffset, inSize)
                program.step()
            }
            OpCode.CALL,
            OpCode.CALLCODE,
            OpCode.DELEGATECALL,
            OpCode.STATICCALL -> {
                program.stackPop() // use adjustedCallGas instead of requested
                val codeAddress = program.stackPop()
                val value = if (op.callHasValue())
                    program.stackPop()
                else
                    DataWord.ZERO

                if (program.isStaticCall() && op === OpCode.CALL && !value.isZero())
                    throw Program.StaticCallModificationException()

                if (!value.isZero()) {
                    adjustedCallGas!!.add(DataWord(gasCosts.getSTIPEND_CALL()))
                }

                val inDataOffs = program.stackPop()
                val inDataSize = program.stackPop()

                val outDataOffs = program.stackPop()
                val outDataSize = program.stackPop()

                if (logger.isInfoEnabled) {
                    hint = ("addr: " + Hex.toHexString(codeAddress.getLast20Bytes())
                            + " gas: " + adjustedCallGas!!.shortHex()
                            + " inOff: " + inDataOffs.shortHex()
                            + " inSize: " + inDataSize.shortHex())
                    logger.info(logString, String.format("%5s", "[" + program.getPC() + "]"),
                            String.format("%-12s", op.name),
                            program.getGas().value(),
                            program.getCallDeep(), hint)
                }

                program.memoryExpand(outDataOffs, outDataSize)

                val msg = MessageCall(
                        op, adjustedCallGas!!, codeAddress, value, inDataOffs, inDataSize,
                        outDataOffs, outDataSize)

                val contract = PrecompiledContracts.getContractForAddress(codeAddress)

                if (!op.callIsStateless()) {
                    program.getResult().addTouchAccount(codeAddress.getLast20Bytes())
                }

                if (contract != null) {
                    program.callToPrecompiledAddress(msg, contract)
                } else {
                    program.callToAddress(msg)
                }
                program.step()
            }
            OpCode.RETURN,
            OpCode.REVERT -> {
                val offset = program.stackPop()
                val size = program.stackPop()

                val hReturn = program.memoryChunk(offset.intValueSafe(), size.intValueSafe())
                program.setHReturn(hReturn)

                if (logger.isInfoEnabled)
                    hint = ("data: " + Hex.toHexString(hReturn)
                            + " offset: " + offset.value()
                            + " size: " + size.value())

                program.step()
                program.stop()

                if (op === OpCode.REVERT) {
                    program.getResult().setRevert()
                }
            }
            OpCode.SUICIDE -> {
                if (program.isStaticCall()) throw Program.StaticCallModificationException()

                val address = program.stackPop()
                program.suicide(address)
                program.getResult().addTouchAccount(address.getLast20Bytes())

                if (logger.isInfoEnabled)
                    hint = "address: " + Hex.toHexString(program.getOwnerAddress().getLast20Bytes())

                program.stop()
            }

        }
        program.setPreviouslyExecutedOp(op.`val`())

        if (logger.isInfoEnabled && !op.isCall())
            logger.info(
                logString, String.format("%5s", "[" + program.getPC() + "]"),
                String.format(
                    "%-12s",
                    op.name
                ), program.getGas().value(),
                program.getCallDeep(), hint
            )

        vmCounter++
        program.fullTrace()

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