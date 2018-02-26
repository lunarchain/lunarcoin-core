package io.lunarchain.lunarcoin.vm.program.invoke

import io.lunarchain.lunarcoin.storage.Repository
import lunar.vm.DataWord
import lunar.vm.program.invoke.ProgramInvoke
import java.math.BigInteger
import java.util.*

class ProgramInvokeImpl(): ProgramInvoke {


    private val MAX_MSG_DATA = BigInteger.valueOf(Integer.MAX_VALUE.toLong());
    private var blockStore: Repository? = null

    /**
     * TRANSACTION  env **
     */
    private var address: DataWord? = null
    private var origin: DataWord? = null
    private var caller: DataWord? = null
    private var balance: DataWord? = null
    private var gas: DataWord? = null
    private var gasPrice: DataWord? = null
    private var callValue: DataWord? = null
    private var gasLong: Long? = null

    var msgData: ByteArray? = null

    /**
     * BLOCK  env **
     */

    private var prevHash: DataWord? = null
    private var coinbase: DataWord? = null
    private var timestamp: DataWord? = null
    private var number: DataWord? = null
    private var difficulty: DataWord? = null
    private var gasLimit: DataWord? = null

    private var storage: Map<DataWord, DataWord>? = null

    private var byTransaction = true
    private var byTestingSuite = false
    private var callDeep = 0
    private var isStaticCall = false


    constructor(address: DataWord, origin: DataWord,
                caller: DataWord, balance: DataWord,
                gasPrice: DataWord, gas: DataWord, callValue: DataWord,
                msgData: ByteArray, lastHash: DataWord, coinbase: DataWord,
                timestamp: DataWord, number: DataWord, difficulty: DataWord,
                gasLimit: DataWord, blockStore: Repository, callDeep: Int,
                isStaticCall: Boolean, byTestingSuite: Boolean): this() {
        // Transaction env
        this.address = address
        this.origin = origin
        this.caller = caller
        this.balance = balance
        this.gasPrice = gasPrice
        this.gas = gas
        this.gasLong = this.gas!!.longValueSafe()
        this.callValue = callValue
        this.msgData = msgData

        // last Block env
        this.prevHash = lastHash
        this.coinbase = coinbase
        this.timestamp = timestamp
        this.number = number
        this.difficulty = difficulty
        this.gasLimit = gasLimit

        this.byTransaction = false
        this.callDeep = callDeep
        this.blockStore = blockStore
        this.isStaticCall = isStaticCall
        this.byTestingSuite = byTestingSuite
    }

    constructor(address: ByteArray, origin: ByteArray,
                caller: ByteArray, balance: ByteArray,
                gasPrice: ByteArray, gas: ByteArray, callValue: ByteArray,
                msgData: ByteArray, lastHash: ByteArray, coinbase: ByteArray,
                timestamp: Long, number: Long, difficulty: ByteArray,
                gasLimit: ByteArray, blockStore: Repository): this() {
        // Transaction env
        this.address = DataWord(address)
        this.origin = DataWord(origin)
        this.caller = DataWord(caller)
        this.balance = DataWord(balance)
        this.gasPrice = DataWord(gasPrice)
        this.gas = DataWord(gas)
        this.gasLong = this.gas!!.longValueSafe()
        this.callValue = DataWord(callValue)
        this.msgData = msgData

        // last Block env
        this.prevHash = DataWord(lastHash)
        this.coinbase = DataWord(coinbase)
        this.timestamp = DataWord(timestamp)
        this.number = DataWord(number)
        this.difficulty = DataWord(difficulty)
        this.gasLimit = DataWord(gasLimit)

        this.blockStore = blockStore
    }

    constructor(address: ByteArray, origin: ByteArray,
                caller: ByteArray, balance: ByteArray,
                gasPrice: ByteArray, gas: ByteArray, callValue: ByteArray,
                msgData: ByteArray, lastHash: ByteArray, coinbase: ByteArray,
                timestamp: Long, number: Long, difficulty: ByteArray,
                gasLimit: ByteArray, blockStore: Repository, byTestingSuite: Boolean): this(address, origin, caller, balance, gasPrice, gas, callValue, msgData, lastHash, coinbase,
    timestamp, number, difficulty, gasLimit, blockStore) {
        this.byTestingSuite = byTestingSuite
    }


    /*           ADDRESS op         */
    override fun getOwnerAddress(): DataWord {
        return this.address!!
    }

    /*           BALANCE op         */
    fun getBalance(): DataWord {
        return this.balance!!
    }

    /*           ORIGIN op         */
    override fun getOriginAddress(): DataWord {
        return this.origin!!
    }

    /*           CALLER op         */
    override fun getCallerAddress(): DataWord {
        return this.caller!!
    }

    /*           GASPRICE op       */
    override fun getMinGasPrice(): DataWord {
        return this.gasPrice!!
    }

    override fun getGas(): DataWord {
        return this.gas!!
    }

    override fun getCallValue(): DataWord {
        return this.callValue!!
    }

    override fun getGasLong(): Long {
        return this.gasLong!!
    }

    override fun getDataValue(indexData: DataWord): DataWord {
        val tempIndex = indexData.value()
        val index = tempIndex.toInt() // possible overflow is caught below
        var size = 32 // maximum datavalue size

        if (msgData == null || index >= msgData!!.size
            || tempIndex.compareTo(MAX_MSG_DATA) == 1
        )
            return DataWord()
        if (index + size > msgData!!.size)
            size = msgData!!.size - index

        val data = ByteArray(32)
        System.arraycopy(msgData, index, data, 0, size)
        return DataWord(data)
    }

    override fun getDataSize(): DataWord {
        if (msgData == null || msgData!!.isEmpty()) return DataWord.ZERO
        val size = msgData!!.size
        return DataWord(size)
    }

    override fun getDataCopy(offsetData: DataWord, lengthData: DataWord): ByteArray {
        val offset = offsetData.intValueSafe()
        var length = lengthData.intValueSafe()

        val data = ByteArray(length)

        if (msgData == null) return data
        if (offset > msgData!!.size) return data
        if (offset + length > msgData!!.size) length = msgData!!.size - offset

        System.arraycopy(msgData, offset, data, 0, length)

        return data
    }

    override fun getPrevHash(): DataWord {
        return this.prevHash!!
    }

    override fun getCoinbase(): DataWord {
        return this.coinbase!!
    }

    override fun getTimestamp(): DataWord {
        return this.timestamp!!
    }

    override fun getNumber(): DataWord {
        return this.number!!
    }

    override fun getDifficulty(): DataWord {
        return this.difficulty!!
    }

    override fun getCallDeep(): Int {
        return this.callDeep
    }

    override fun isStaticCall(): Boolean {
        return this.isStaticCall
    }

    /*     GASLIMIT op    */
    override fun getGaslimit(): DataWord {
        return this.gasLimit!!
    }

    /*  Storage */
    fun getStorage(): Map<DataWord, DataWord> {
        return this.storage!!
    }

    override fun getRepository(): Repository {
        return this.blockStore!!
    }

    fun getBlockStore(): Repository {
        return this.blockStore!!
    }

    fun byTransaction(): Boolean {
        return byTransaction
    }

    override fun byTestingSuite(): Boolean {
        return byTestingSuite
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as ProgramInvokeImpl?

        if (byTestingSuite != that!!.byTestingSuite) return false
        if (byTransaction != that.byTransaction) return false
        if (if (address != null) !address!!.equals(that.address) else that.address != null) return false
        if (if (balance != null) !balance!!.equals(that.balance) else that.balance != null) return false
        if (if (callValue != null) !callValue!!.equals(that.callValue) else that.callValue != null) return false
        if (if (caller != null) !caller!!.equals(that.caller) else that.caller != null) return false
        if (if (coinbase != null) !coinbase!!.equals(that.coinbase) else that.coinbase != null) return false
        if (if (difficulty != null) !difficulty!!.equals(that.difficulty) else that.difficulty != null) return false
        if (if (gas != null) !gas!!.equals(that.gas) else that.gas != null) return false
        if (if (gasPrice != null) !gasPrice!!.equals(that.gasPrice) else that.gasPrice != null) return false
        if (if (gasLimit != null) !gasLimit!!.equals(that.gasLimit) else that.gasLimit != null) return false
        if (!Arrays.equals(msgData, that.msgData)) return false
        if (if (number != null) !number!!.equals(that.number) else that.number != null) return false
        if (if (origin != null) !origin!!.equals(that.origin) else that.origin != null) return false
        if (if (prevHash != null) !prevHash!!.equals(that.prevHash) else that.prevHash != null) return false
        if (if (blockStore != null) !blockStore!!.equals(that.blockStore) else that.blockStore != null) return false
        if (if (storage != null) storage != that.storage else that.storage != null) return false
        return if (if (timestamp != null) !timestamp!!.equals(that.timestamp) else that.timestamp != null) false else true

    }

    override fun toString(): String {
        return "ProgramInvokeImpl{" +
                "address=" + address +
                ", origin=" + origin +
                ", caller=" + caller +
                ", balance=" + balance +
                ", gas=" + gas +
                ", gasPrice=" + gasPrice +
                ", callValue=" + callValue +
                ", msgData=" + Arrays.toString(msgData) +
                ", prevHash=" + prevHash +
                ", coinbase=" + coinbase +
                ", timestamp=" + timestamp +
                ", number=" + number +
                ", difficulty=" + difficulty +
                ", gaslimit=" + gasLimit +
                ", storage=" + storage +
                ", repository=" + blockStore +
                ", byTransaction=" + byTransaction +
                ", byTestingSuite=" + byTestingSuite +
                ", callDeep=" + callDeep +
                '}'.toString()
    }


}