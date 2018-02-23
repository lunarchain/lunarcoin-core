package io.lunarchain.lunarcoin.storage

import io.lunarchain.lunarcoin.core.AccountState
import io.lunarchain.lunarcoin.core.AccountWithKey
import io.lunarchain.lunarcoin.core.Block
import io.lunarchain.lunarcoin.core.Transaction
import lunar.vm.DataWord
import java.math.BigInteger

interface Repository {
    /**
     * 读取账户余额。
     */
    fun getBalance(address: ByteArray): BigInteger

    /**
     * 账户的Nonce+1
     */
    fun increaseNonce(address: ByteArray)

    /**
     * 增加账户余额。
     */
    fun addBalance(address: ByteArray, amount: BigInteger)

    fun saveAccount(account: AccountWithKey, password: String = ""): Int
    fun getAccount(index: Int, password: String = ""): AccountWithKey?
    fun accountNumber(): Int
    fun getBlockInfo(hash: ByteArray): BlockInfo?
    fun getBlockInfos(height: Long): List<BlockInfo>?
    fun getBlock(hash: ByteArray): Block?
    fun saveBlock(block: Block)
    fun getBestBlock(): Block?
    fun updateBestBlock(block: Block)
    fun putTransaction(trx: Transaction)
    fun close()
    fun changeAccountStateRoot(stateRoot: ByteArray)
    fun getAccountStateRoot(): ByteArray?
    fun updateBlockInfo(height: Long, blockInfo: BlockInfo)
    fun isExist(address: ByteArray): Boolean
    fun getStorageValue(addr: ByteArray, key: DataWord): DataWord
    fun getAccountState(address: ByteArray): AccountState

}

