package lunar.vm.program.invoke

import io.lunarchain.lunarcoin.storage.Repository
import lunar.vm.DataWord

interface ProgramInvoke {
     fun getCallerAddress(): DataWord
     fun getOriginAddress(): DataWord
     fun getOwnerAddress(): DataWord
     fun getRepository(): Repository
     fun getGas(): DataWord
     fun byTestingSuite(): Boolean
     fun getCallDeep(): Int
     fun getGasLong(): Long
     fun getCallValue(): DataWord
     fun getDataValue(indexData: DataWord): DataWord
     fun getDataSize(): DataWord
     fun getDataCopy(offsetData: DataWord, lengthData: DataWord): ByteArray
     fun getMinGasPrice(): DataWord
     fun getPrevHash(): DataWord
     fun getNumber(): DataWord
     fun getCoinbase(): DataWord
     fun getTimestamp(): DataWord
     fun getDifficulty(): DataWord
     fun getGaslimit(): DataWord
     fun isStaticCall(): Boolean

}