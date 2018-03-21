package lunar.vm.program

import io.lunarchain.lunarcoin.util.ByteArraySet
import io.lunarchain.lunarcoin.util.ByteArrayWrapper
import io.lunarchain.lunarcoin.util.ByteUtil.EMPTY_BYTE_ARRAY
import io.lunarchain.lunarcoin.vm.CallCreate
import io.lunarchain.lunarcoin.vm.LogInfo
import lunar.vm.DataWord
import java.util.*

class ProgramResult {

    //program 消耗的GAS统计
    private var gasUsed: Long = 0

    private var exception: RuntimeException? = null

    //是否回滚
    private var revert: Boolean = false

    private var deleteAccounts: MutableSet<DataWord> = HashSet<DataWord>()

    private val touchedAccounts = ByteArraySet(HashSet<ByteArrayWrapper>())

    private var futureRefund: Long = 0

    private var hReturn = EMPTY_BYTE_ARRAY

    private var logInfoList: MutableList<LogInfo>? = null

    private var callCreateList: MutableList<CallCreate>? = null

    fun spendGas(gas: Long) {
        gasUsed += gas
    }

    fun setRevert() {
        this.revert = true
    }

    fun isRevert(): Boolean {
        return revert
    }

    fun refundGas(gas: Long) {
        gasUsed -= gas
    }

    fun setException(exception: RuntimeException) {
        this.exception = exception
    }

    fun getException(): RuntimeException? {
        return exception
    }

    fun getGasUsed(): Long {
        return gasUsed
    }

    fun getDeleteAccounts(): MutableSet<DataWord> {
        return deleteAccounts
    }

    fun addDeleteAccount(address: DataWord) {
        getDeleteAccounts().add(address)
    }

    fun addDeleteAccounts(accounts: Set<DataWord>) {
        if (!accounts.isEmpty()) {
            getDeleteAccounts().addAll(accounts)
        }
    }

    fun addTouchAccount(addr: ByteArray) {
        touchedAccounts.add(addr)
    }

    fun getTouchedAccounts(): MutableSet<ByteArray> {
        return touchedAccounts
    }

    fun addTouchAccounts(accounts: Set<ByteArray>) {
        if (!accounts.isEmpty()) {
            getTouchedAccounts().addAll(accounts)
        }
    }

    fun addFutureRefund(gasValue: Long) {
        futureRefund += gasValue
    }

    fun setHReturn(hReturn: ByteArray) {
        this.hReturn = hReturn

    }

    fun getLogInfoList(): MutableList<LogInfo> {
        if (logInfoList == null) {
            logInfoList = ArrayList()
        }
        return logInfoList!!
    }


    fun addLogInfo(logInfo: LogInfo) {
        getLogInfoList().add(logInfo)
    }

    fun getHReturn(): ByteArray {
        return hReturn
    }

    fun getCallCreateList(): MutableList<CallCreate> {
        if (callCreateList == null) {
            callCreateList = ArrayList()
        }
        return callCreateList!!
    }

    fun addCallCreate(data: ByteArray, destination: ByteArray, gasLimit: ByteArray, value: ByteArray) {
        getCallCreateList().add(CallCreate(data, destination, gasLimit, value))
    }



}