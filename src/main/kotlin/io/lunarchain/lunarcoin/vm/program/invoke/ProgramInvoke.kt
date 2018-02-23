package lunar.vm.program.invoke

import io.lunarchain.lunarcoin.storage.Repository
import lunar.vm.DataWord

interface ProgramInvoke {
    abstract fun getOwnerAddress(): DataWord
    abstract fun getRepository(): Repository
    abstract fun getGas(): DataWord
    abstract fun byTestingSuite(): Boolean
    abstract fun getCallDeep(): Int
    abstract fun getGasLong(): Long

}