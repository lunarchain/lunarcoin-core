package io.lunarchain.lunarcoin.storage

import io.lunarchain.lunarcoin.core.AccountState
import io.lunarchain.lunarcoin.core.AccountWithKey
import io.lunarchain.lunarcoin.core.Block
import io.lunarchain.lunarcoin.core.Transaction
import io.lunarchain.lunarcoin.trie.PatriciaTrie
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

    fun addBalanceWithResult(address: ByteArray, amount: BigInteger): BigInteger

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
    fun getStorageValue(addr: ByteArray, key: DataWord): DataWord?
    fun getAccountState(address: ByteArray): AccountState?
    fun getAccountStateStore(): PatriciaTrie?
    fun getOrCreateAccountState(addr: ByteArray): AccountState
    fun createAccountState(addr: ByteArray): AccountState
    fun delete(address: ByteArray)
    fun createAccountStorage(address: ByteArray)

    /**
     * Retrieve the code associated with an account
     *
     * @param addr of the account
     * @return code in byte-array format
     */
    fun getCode(addr: ByteArray): ByteArray?


    fun saveCode(addr: ByteArray, code: ByteArray)


    fun getCodeHash(addr: ByteArray): ByteArray?

    fun getNonce(addr: ByteArray): BigInteger

    fun setNonce(addr: ByteArray, nonce: BigInteger)

    fun getContractDetails(addr: ByteArray): ContractDetails {
        return ContractDetailsImpl(addr, this)
    }

    fun startTracking()

    fun commit()

    fun rollback()


    /**
     * Gets the block hash by its index.
     * When more than one block with the specified index exists (forks)
     * the select the block which is ancestor of the branchBlockHash
     */
    fun getBlockHashByNumber(blockNumber: Long, branchBlockHash: ByteArray): ByteArray

    /**
     * Put a value in storage of an account at a given key
     *
     * @param addr of the account
     * @param key of the data to store
     * @param value is the data to store
     */
    fun addStorageRow(addr: ByteArray, key: DataWord, value: DataWord)

}

