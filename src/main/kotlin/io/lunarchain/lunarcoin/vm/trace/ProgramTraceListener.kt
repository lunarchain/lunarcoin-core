package lunar.vm.trace

import lunar.vm.DataWord
import lunar.vm.program.listener.ProgramListenerAdaptor

class ProgramTraceListener(private val enabled: Boolean): ProgramListenerAdaptor() {
    private var actions = OpActions()

    override fun onMemoryExtend(delta: Int) {
        if (enabled) actions.addMemoryExtend(delta.toLong())
    }

    override fun onMemoryWrite(address: Int, data: ByteArray, size: Int) {
        if (enabled) actions.addMemoryWrite(address, data, size)
    }

    override fun onStackPop() {
        if (enabled) actions.addStackPop()
    }

    override fun onStackPush(value: DataWord) {
        if (enabled) actions.addStackPush(value)
    }

    override fun onStackSwap(from: Int, to: Int) {
        if (enabled) actions.addStackSwap(from, to)
    }

    override fun onStoragePut(key: DataWord, value: DataWord) {
        if (enabled) {
            if (value == (DataWord.ZERO)) {
                actions.addStorageRemove(key)
            } else {
                actions.addStoragePut(key, value)
            }
        }
    }

    override fun onStorageClear() {
        if (enabled) actions.addStorageClear()
    }

    fun resetActions(): OpActions {
        val actions = this.actions
        this.actions = OpActions()
        return actions
    }
}