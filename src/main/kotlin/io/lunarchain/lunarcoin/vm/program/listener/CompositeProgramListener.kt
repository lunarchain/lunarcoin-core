package lunar.vm.program.listener

import lunar.vm.DataWord
import java.util.*

class CompositeProgramListener: ProgramListener {

    private val listeners: ArrayList<ProgramListener> = ArrayList()

    override fun onMemoryExtend(delta: Int) {
        for (listener in listeners) {
            listener.onMemoryExtend(delta)
        }
    }

    override fun onMemoryWrite(address: Int, data: ByteArray, size: Int) {
        for (listener in listeners) {
            listener.onMemoryWrite(address, data, size)
        }
    }

    override fun onStackPop() {
        for (listener in listeners) {
            listener.onStackPop()
        }
    }

    override fun onStackPush(value: DataWord) {
        for (listener in listeners) {
            listener.onStackPush(value)
        }
    }

    override fun onStackSwap(from: Int, to: Int) {
        for (listener in listeners) {
            listener.onStackSwap(from, to)
        }
    }

    override fun onStoragePut(key: DataWord, value: DataWord) {
        for (listener in listeners) {
            listener.onStoragePut(key, value)
        }
    }

    override fun onStorageClear() {
        for (listener in listeners) {
            listener.onStorageClear()
        }
    }

    fun addListener(listener: ProgramListener) {
        listeners.add(listener)
    }

    fun isEmpty(): Boolean {
        return listeners.isEmpty()
    }
}