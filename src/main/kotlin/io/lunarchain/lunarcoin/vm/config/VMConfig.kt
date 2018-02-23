package lunar.vm.config

import io.lunarchain.lunarcoin.util.Utils
import lunar.vm.DataWord
import lunar.vm.OpCode
import lunar.vm.program.Program

object VMConfig {
    @Throws(Program.OutOfGasException::class)
    fun getCallGas(op: OpCode, requestedGas: DataWord, availableGas: DataWord): DataWord {
        val maxAllowed = Utils.allButOne64th(availableGas)
        return if (requestedGas.compareTo(maxAllowed) > 0) maxAllowed else requestedGas
    }
}