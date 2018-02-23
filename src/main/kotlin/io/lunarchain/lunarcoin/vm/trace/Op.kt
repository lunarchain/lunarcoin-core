package lunar.vm.trace

import lunar.vm.OpCode
import java.math.BigInteger

class Op {

    private var code: OpCode? = null
    private var deep: Int = 0
    private var pc: Int = 0
    private var gas: BigInteger? = null
    private var actions: OpActions? = null

    fun getCode(): OpCode? {
        return code
    }

    fun setCode(code: OpCode?) {
        this.code = code
    }

    fun getDeep(): Int {
        return deep
    }

    fun setDeep(deep: Int) {
        this.deep = deep
    }

    fun getPc(): Int {
        return pc
    }

    fun setPc(pc: Int) {
        this.pc = pc
    }

    fun getGas(): BigInteger? {
        return gas
    }

    fun setGas(gas: BigInteger) {
        this.gas = gas
    }

    fun getActions(): OpActions? {
        return actions
    }

    fun setActions(actions: OpActions) {
        this.actions = actions
    }
}