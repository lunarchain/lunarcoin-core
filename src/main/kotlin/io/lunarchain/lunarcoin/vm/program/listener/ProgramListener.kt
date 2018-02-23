package lunar.vm.program.listener

import lunar.vm.DataWord

interface ProgramListener {
    abstract fun onMemoryExtend(delta: Int)

    abstract fun onMemoryWrite(address: Int, data: ByteArray, size: Int)

    abstract fun onStackPop()

    abstract fun onStackPush(value: DataWord)

    abstract fun onStackSwap(from: Int, to: Int)

    abstract fun onStoragePut(key: DataWord, value: DataWord)

    abstract fun onStorageClear()
}