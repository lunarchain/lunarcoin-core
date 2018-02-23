package lunar.vm.program

import io.lunarchain.lunarcoin.core.Transaction
import io.lunarchain.lunarcoin.util.ByteArraySet
import io.lunarchain.lunarcoin.util.ByteUtil.EMPTY_BYTE_ARRAY
import io.lunarchain.lunarcoin.util.Utils
import lunar.vm.DataWord
import lunar.vm.OpCode
import lunar.vm.program.invoke.ProgramInvoke
import lunar.vm.program.listener.CompositeProgramListener
import lunar.vm.program.listener.ProgramListenerAware
import lunar.vm.program.listener.ProgramStorageChangeListener
import lunar.vm.trace.ProgramTrace
import lunar.vm.trace.ProgramTraceListener
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex
import java.lang.StrictMath.min
import java.lang.String.format
import java.lang.reflect.Array.getLength
import java.math.BigInteger
import java.util.*

class Program(private val ops: ByteArray, private val programInvoke: ProgramInvoke, private val transaction: Transaction) {

    private val logger = LoggerFactory.getLogger(javaClass)

    //VM stack depth
    private val MAX_DEPTH = 2048

    //VM stack size, 这里做了更改
    private val MAX_STACKSIZE = 2048

    private val result = ProgramResult()
    //TODO 默认TRACE开启，后期需写入配置文件
    private val trace = ProgramTrace(true, programInvoke)

    private val codeHash: ByteArray? = null
    private var pc: Int = 0
    private var lastOp: Byte = 0
    private var previouslyExecutedOp: Byte = 0
    private var stopped: Boolean = false
    private val touchedAccounts = ByteArraySet(HashSet())

    private val stack: Stack = setupProgramListener(Stack())
    private val memory: Memory = setupProgramListener(Memory())
    private val storage: Storage = setupProgramListener(Storage(programInvoke))

    //TODO 默认TRACE开启，后期需写入配置文件
    private val traceListener = ProgramTraceListener(true)
    private val storageDiffListener = ProgramStorageChangeListener()
    private val programListener = CompositeProgramListener()

    private var returnDataBuffer: ByteArray? = null


    private fun <T : ProgramListenerAware> setupProgramListener(programListenerAware: T): T {
        if (programListener.isEmpty()) {
            programListener.addListener(traceListener)
            programListener.addListener(storageDiffListener)
        }

        programListenerAware.setProgramListener(programListener)

        return programListenerAware
    }


    fun getOp(pc: Int): Byte {
        return if (getLength(ops) <= pc) 0 else ops[pc]
    }

    fun getCurrentOp(): Byte {
        return if (ops.isEmpty()) 0 else ops[pc]
    }

    fun stop() {
        stopped = true
    }

    /**
     * Last Op can only be set publicly (no getLastOp method), is used for logging.
     */
    fun setLastOp(op: Byte) {
        this.lastOp = op
    }

    fun getPC(): Int {
        return pc
    }

    fun setPC(pc: DataWord) {
        this.setPC(pc.intValue())
    }

    fun setPC(pc: Int) {
        this.pc = pc

        if (this.pc >= ops.size) {
            stop()
        }
    }

    fun step() {
        setPC(pc + 1)
    }

    fun sweep(n: Int): ByteArray {

        if (pc + n > ops.size)
            stop()

        val data = Arrays.copyOfRange(ops, pc, pc + n)
        pc += n
        if (pc >= ops.size) stop()

        return data
    }

    fun getStack(): Stack {
        return this.stack
    }

    fun stackPop(): DataWord {
        return stack.pop()
    }

    fun setPreviouslyExecutedOp(op: Byte) {
        this.previouslyExecutedOp = op
    }

    fun getPreviouslyExecutedOp(): Byte {
        return this.previouslyExecutedOp
    }

    fun getBalance(address: DataWord): DataWord {
        val balance = getStorage().getBalance(address.getLast20Bytes())
        return DataWord(balance?.toByteArray())
    }

    /**
     * Verifies that the stack is at least `stackSize`
     *
     * @param stackSize int
     * @throws StackTooSmallException If the stack is
     * smaller than `stackSize`
     */
    fun verifyStackSize(stackSize: Int) {
        if (stack.size < stackSize) {
            throw Program.tooSmallStack(stackSize, stack.size)
        }
    }

    fun verifyStackOverflow(argsReqs: Int, returnReqs: Int) {
        if (stack.size - argsReqs + returnReqs > MAX_STACKSIZE) {
            throw StackTooLargeException("Expected: overflow $MAX_STACKSIZE elements stack limit")
        }
    }

    fun stackPush(stackWord: DataWord) {
        verifyStackOverflow(0, 1) //Sanity Check
        stack.push(stackWord)
    }

    fun stackPush(data: ByteArray) {
        stackPush(DataWord(data))
    }

    fun getResult(): ProgramResult {
        return result
    }

    fun getMemSize(): Int {
        return memory.size()
    }

    fun memorySave(addrB: DataWord, value: DataWord) {
        memory.write(addrB.intValue(), value.getData(), value.getData().size, false)
    }

    fun memorySaveLimited(addr: Int, data: ByteArray, dataSize: Int) {
        memory.write(addr, data, dataSize, true)
    }

    fun memorySave(addr: Int, value: ByteArray) {
        memory.write(addr, value, value.size, false)
    }

    fun memoryExpand(outDataOffs: DataWord, outDataSize: DataWord) {
        if (!outDataSize.isZero()) {
            memory.extend(outDataOffs.intValue(), outDataSize.intValue())
        }
    }

    /**
     * Allocates a piece of memory and stores value at given offset address
     *
     * @param addr      is the offset address
     * @param allocSize size of memory needed to write
     * @param value     the data to write to memory
     */
    fun memorySave(addr: Int, allocSize: Int, value: ByteArray) {
        memory.extendAndWrite(addr, allocSize, value)
    }


    fun memoryLoad(addr: DataWord): DataWord {
        return memory.readWord(addr.intValue())
    }

    fun memoryLoad(address: Int): DataWord {
        return memory.readWord(address)
    }

    fun memoryChunk(offset: Int, size: Int): ByteArray {
        return memory.read(offset, size)
    }

    /**
     * Allocates extra memory in the program for
     * a specified size, calculated from a given offset
     *
     * @param offset the memory address offset
     * @param size   the number of bytes to allocate
     */
    fun allocateMemory(offset: Int, size: Int) {
        memory.extend(offset, size)
    }

    fun getStorage(): Storage {
        return this.storage
    }


    fun byTestingSuite(): Boolean {
        return programInvoke.byTestingSuite()
    }

    fun isStopped(): Boolean {
        return stopped
    }

    fun setRuntimeFailure(e: RuntimeException) {
        getResult().setException(e)
    }

    fun getCallDeep(): Int {
        return programInvoke.getCallDeep()
    }

    fun getGas(): DataWord {
        return DataWord(programInvoke.getGasLong() - getResult().getGasUsed())
    }

    fun saveOpTrace() {
        if (this.pc < ops.size) {
            trace.addOp(ops[pc], pc, getCallDeep(), getGas(), traceListener.resetActions())
        }
    }

    fun getTrace(): ProgramTrace {
        return trace
    }

    fun getGasLong(): Long {
        return programInvoke.getGasLong() - getResult().getGasUsed()
    }

    fun getOwnerAddress(): DataWord {
        return programInvoke.getOwnerAddress().clone()
    }

    fun storageLoad(key: DataWord): DataWord? {
        val ret = getStorage().getStorageValue(getOwnerAddress().getLast20Bytes(), key.clone())
        return ret?.clone()
    }

    fun futureRefundGas(gasValue: Long) {
        logger.info("Future refund added: [{}]", gasValue)
        getResult().addFutureRefund(gasValue)
    }

    fun spendGas(gasValue: Long, cause: String) {
        if (logger.isDebugEnabled) {
            logger.debug("[{}] Spent for cause: [{}], gas: [{}]", programInvoke.hashCode(), cause, gasValue)
        }

        if (getGasLong() < gasValue) {
            throw Program.notEnoughSpendingGas(cause, gasValue, this)
        }
        getResult().spendGas(gasValue)
    }

    fun setHReturn(buff: ByteArray) {
        getResult().setHReturn(buff)
    }

    fun getOriginAddress(): DataWord {
        return programInvoke.getOriginAddress().clone()
    }

    fun getCallerAddress(): DataWord {
        return programInvoke.getCallerAddress().clone()
    }

    fun getCallValue(): DataWord {
        return programInvoke.getCallValue().clone()
    }

    fun getDataValue(index: DataWord): DataWord {
        return programInvoke.getDataValue(index)
    }

    fun getDataSize(): DataWord {
        return programInvoke.getDataSize().clone()
    }

    fun getDataCopy(offset: DataWord, length: DataWord): ByteArray {
        return programInvoke.getDataCopy(offset, length)
    }

    fun getReturnDataBufferSize(): DataWord {
        return DataWord(getReturnDataBufferSizeI())
    }

    private fun getReturnDataBufferSizeI(): Int {
        return if (returnDataBuffer == null) 0 else returnDataBuffer!!.size
    }

    fun getReturnDataBufferData(off: DataWord, size: DataWord): ByteArray? {
        if (off.intValueSafe() as Long + size.intValueSafe() > getReturnDataBufferSizeI()) return null
        return if (returnDataBuffer == null)
            ByteArray(0)
        else
            Arrays.copyOfRange(returnDataBuffer, off.intValueSafe(), off.intValueSafe() + size.intValueSafe())
    }

    fun getCode(): ByteArray {
        return ops
    }

    fun getCodeAt(address: DataWord): ByteArray {
        val code: ByteArray? = programInvoke.getRepository().getCode(address.getLast20Bytes())
        if(code == null || code.isEmpty()) return EMPTY_BYTE_ARRAY else return code
    }

    fun getGasPrice(): DataWord {
        return programInvoke.getMinGasPrice().clone()
    }

    fun getNumber(): DataWord {
        return programInvoke.getNumber().clone()
    }

    fun getPrevHash(): DataWord {
        return programInvoke.getPrevHash().clone()
    }

    fun getBlockHash(index: Int): DataWord {
        return if (index < this.getNumber().longValue() && index >= Math.max(256, this.getNumber().intValue()) - 256)
            DataWord(this.programInvoke.getRepository().getBlockHashByNumber(index.toLong(), getPrevHash().getData())).clone()
        else
            DataWord.ZERO.clone()
    }

    fun getCoinbase(): DataWord {
        return programInvoke.getCoinbase().clone()
    }

    fun getTimestamp(): DataWord {
        return programInvoke.getTimestamp().clone()
    }

    fun getDifficulty(): DataWord {
        return programInvoke.getDifficulty().clone()
    }

    fun getGasLimit(): DataWord {
        return programInvoke.getGaslimit().clone()
    }

    fun isStaticCall(): Boolean {
        return programInvoke.isStaticCall()
    }

    fun storageSave(key: ByteArray, `val`: ByteArray) {
        val keyWord = DataWord(key)
        val valWord = DataWord(`val`)
        getStorage().addStorageRow(getOwnerAddress().getLast20Bytes(), keyWord, valWord)
    }

    fun storageSave(word1: DataWord, word2: DataWord) {
        storageSave(word1.getData(), word2.getData())
    }

    companion object {

        fun formatBinData(binData: ByteArray, startPC: Int): String {
            val ret = StringBuilder()
            var i = 0
            while (i < binData.size) {
                ret.append(Utils.align("" + Integer.toHexString(startPC + i) + ":", ' ', 8, false))
                ret.append(Hex.toHexString(binData, i, min(16, binData.size - i))).append('\n')
                i += 16
            }
            return ret.toString()
        }

        fun notEnoughOpGas(op: OpCode, opGas: Long, programGas: Long): OutOfGasException {
            return OutOfGasException("Not enough gas for '%s' operation executing: opGas[%d], programGas[%d];", op, opGas, programGas)
        }

        fun notEnoughOpGas(op: OpCode, opGas: DataWord, programGas: DataWord): OutOfGasException {
            return notEnoughOpGas(op, opGas.longValue(), programGas.longValue())
        }

        fun notEnoughOpGas(op: OpCode, opGas: BigInteger, programGas: BigInteger): OutOfGasException {
            return notEnoughOpGas(op, opGas.toLong(), programGas.toLong())
        }

        fun notEnoughSpendingGas(cause: String, gasValue: Long, program: Program): OutOfGasException {
            return OutOfGasException("Not enough gas for '%s' cause spending: invokeGas[%d], gas[%d], usedGas[%d];",
                    cause, program.programInvoke.getGas().longValue(), gasValue, program.getResult().getGasUsed())
        }

        fun gasOverflow(actualGas: BigInteger, gasLimit: BigInteger): OutOfGasException {
            return OutOfGasException("Gas value overflow: actualGas[%d], gasLimit[%d];", actualGas.toLong(), gasLimit.toLong())
        }

        fun invalidOpCode(vararg opCode: Byte): IllegalOperationException {
            return IllegalOperationException("Invalid operation code: opCode[%s];", Hex.toHexString(opCode, 0, 1))
        }

        fun badJumpDestination(pc: Int): BadJumpDestinationException {
            return BadJumpDestinationException("Operation with pc isn't 'JUMPDEST': PC[%d];", pc)
        }

        fun tooSmallStack(expectedSize: Int, actualSize: Int): StackTooSmallException {
            return StackTooSmallException("Expected stack size %d but actual %d;", expectedSize, actualSize)
        }
    }

    open class BytecodeExecutionException(message: String) : RuntimeException(message)

    inner class StackTooLargeException(message: String) : BytecodeExecutionException(message)
    class StaticCallModificationException : BytecodeExecutionException("Attempt to call a state modifying opcode inside STATICCALL")
    class ReturnDataCopyIllegalBoundsException(off: DataWord, size: DataWord, returnDataSize: Long) : BytecodeExecutionException(String.format("Illegal RETURNDATACOPY arguments: offset (%s) + size (%s) > RETURNDATASIZE (%d)", off, size, returnDataSize))

    class OutOfGasException(message: String, vararg args: Any) : BytecodeExecutionException(format(message, *args))

    class IllegalOperationException(message: String, vararg args: Any) : BytecodeExecutionException(format(message, *args))

    class BadJumpDestinationException(message: String, vararg args: Any) : BytecodeExecutionException(format(message, *args))

    class StackTooSmallException(message: String, vararg args: Any) : BytecodeExecutionException(format(message, *args))

}