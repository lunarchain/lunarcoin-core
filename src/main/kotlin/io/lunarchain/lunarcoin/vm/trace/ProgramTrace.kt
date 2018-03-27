package lunar.vm.trace

import io.lunarchain.lunarcoin.util.ByteUtil.toHexString
import lunar.vm.DataWord
import lunar.vm.OpCode
import lunar.vm.program.invoke.ProgramInvoke
import org.spongycastle.util.encoders.Hex
import java.lang.String.format
import java.util.*

class ProgramTrace(val enabled: Boolean, val programInvoke: ProgramInvoke) {
    private var ops: MutableList<Op> = ArrayList()
    private var result: String? = null
    private var error: String? = null
    private var contractAddress = Hex.toHexString(programInvoke.getOwnerAddress().getLast20Bytes())
    fun getOps(): MutableList<Op> {
        return ops
    }

    fun setOps(ops: MutableList<Op>) {
        this.ops = ops
    }

    fun getResult(): String? {
        return result
    }

    fun setResult(result: String) {
        this.result = result
    }

    fun getError(): String? {
        return error
    }

    fun setError(error: String) {
        this.error = error
    }

    fun getContractAddress(): String {
        return contractAddress
    }

    fun setContractAddress(contractAddress: String) {
        this.contractAddress = contractAddress
    }

    fun result(result: ByteArray): ProgramTrace {
        setResult(toHexString(result))
        return this
    }

    fun error(error: Exception?): ProgramTrace {
        setError(if (error == null) "" else format("%s: %s", error.javaClass, error.message))
        return this
    }

    fun addOp(code: Byte, pc: Int, deep: Int, gas: DataWord, actions: OpActions): Op {
        val op = Op()
        op.setActions(actions)
        op.setCode(OpCode.code(code))
        op.setDeep(deep)
        op.setGas(gas.value())
        op.setPc(pc)

        ops.add(op)

        return op
    }

    fun merge(programTrace: ProgramTrace) {
        this.ops.addAll(programTrace.ops)
    }


}