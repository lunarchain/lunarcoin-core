package lunar.vm.program

import io.lunarchain.lunarcoin.core.AccountState
import io.lunarchain.lunarcoin.storage.Repository
import lunar.vm.DataWord
import lunar.vm.program.invoke.ProgramInvoke
import lunar.vm.program.listener.ProgramListener
import lunar.vm.program.listener.ProgramListenerAware
import java.math.BigInteger

class Storage(val programInvoke: ProgramInvoke): ProgramListenerAware {

    private var address: DataWord? = null
    private var repository: Repository? = null
    private var programListener: ProgramListener? = null

    init {
        address = programInvoke.getOwnerAddress()
        repository = programInvoke.getRepository()
    }

    override fun setProgramListener(listener: ProgramListener) {
        this.programListener = listener
    }

    fun isExist(addr: ByteArray): Boolean {
        if(repository == null) return false
        return repository!!.isExist(addr)
    }

    fun getRepository(): Repository {
        return this.repository!!
    }


    fun getAccountState(addr: ByteArray): AccountState? {
        return repository!!.getAccountState(addr)
    }

    fun getBalance(addr: ByteArray): BigInteger? {
        return repository!!.getBalance(addr)
    }

    fun addBalance(addr: ByteArray, amount: BigInteger) {
        repository!!.addBalance(addr, amount)
    }

    fun getStorageValue(addr: ByteArray, key: DataWord): DataWord? {
        return repository?.getStorageValue(addr, key)
    }

    fun addStorageRow(addr: ByteArray, key: DataWord, value: DataWord) {
        repository!!.addStorageRow(addr, key, value)
    }

    fun getNonce(addr: ByteArray): BigInteger {
        return repository!!.getNonce(addr)
    }

    fun increaseNonce(addr: ByteArray): BigInteger {
        repository!!.increaseNonce(addr)
        return repository!!.getNonce(addr)
    }

    fun getCode(addr: ByteArray): ByteArray? {
        return repository!!.getCode(addr)
    }


}