package io.lunarchain.lunarcoin.storage

import lunar.vm.DataWord

interface ContractDetails {
    fun put(key: DataWord, value: DataWord)

    operator fun get(key: DataWord): DataWord

    fun getCode(): ByteArray

    fun getCode(codeHash: ByteArray): ByteArray

    fun setCode(code: ByteArray)

    fun getStorageHash(): ByteArray

    fun decode(rlpCode: ByteArray)

    fun setDirty(dirty: Boolean)

    fun setDeleted(deleted: Boolean)

    fun isDirty(): Boolean

    fun isDeleted(): Boolean

    fun getEncoded(): ByteArray

    fun getStorageSize(): Int

    fun getStorageKeys(): Set<DataWord>

    fun getStorage(keys: Collection<DataWord>?): Map<DataWord, DataWord>

    fun getStorage(): Map<DataWord, DataWord>

    fun setStorage(storageKeys: List<DataWord>, storageValues: List<DataWord>)

    fun setStorage(storage: Map<DataWord, DataWord>)

    fun getAddress(): ByteArray

    fun setAddress(address: ByteArray)

    fun clone(): ContractDetails

    override fun toString(): String

    fun syncStorage()

    fun getSnapshotTo(hash: ByteArray): ContractDetails
}