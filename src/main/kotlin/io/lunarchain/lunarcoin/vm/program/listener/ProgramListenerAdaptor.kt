package lunar.vm.program.listener

import lunar.vm.DataWord

open class ProgramListenerAdaptor: ProgramListener {
    override fun onMemoryExtend(delta: Int) {

    }

    override fun onMemoryWrite(address: Int, data: ByteArray, size: Int) {

    }

    override fun onStackPop() {

    }

    override fun onStackPush(value: DataWord) {

    }

    override fun onStackSwap(from: Int, to: Int) {

    }

    override fun onStoragePut(key: DataWord, value: DataWord) {

    }

    override fun onStorageClear() {

    }
}