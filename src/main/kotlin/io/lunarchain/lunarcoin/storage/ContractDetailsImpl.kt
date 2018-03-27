package io.lunarchain.lunarcoin.storage

import lunar.vm.DataWord

class ContractDetailsImpl(private val address: ByteArray, private val repo: Repository): ContractDetails {

    override fun put(key: DataWord, value: DataWord) {
        repo.addStorageRow(address, key, value)
    }

    override operator fun get(key: DataWord): DataWord {
        return repo.getStorageValue(address, key)!!
    }

    override fun getCode(): ByteArray {
        return repo.getCode(address)!!
    }

    override fun getCode(codeHash: ByteArray): ByteArray {
        throw RuntimeException("Not supported")
    }

    override fun setCode(code: ByteArray) { repo.saveCode(address, code)
    }

    override fun getStorageHash(): ByteArray {
        throw RuntimeException("Not supported")
    }

    override fun decode(rlpCode: ByteArray) {
        throw RuntimeException("Not supported")
    }

    override fun setDirty(dirty: Boolean) {
        throw RuntimeException("Not supported")
    }

    override fun setDeleted(deleted: Boolean) {
        repo.delete(address)
    }

    override fun isDirty(): Boolean {
        throw RuntimeException("Not supported")
    }

    override fun isDeleted(): Boolean {
        throw RuntimeException("Not supported")
    }

    override fun getEncoded(): ByteArray {
        throw RuntimeException("Not supported")
    }

    override fun getStorageSize(): Int {
        throw RuntimeException("Not supported")
    }

    override fun getStorageKeys(): Set<DataWord> {
        throw RuntimeException("Not supported")
    }

    override fun getStorage(keys: Collection<DataWord>?): Map<DataWord, DataWord> {
        throw RuntimeException("Not supported")
    }

    override fun getStorage(): Map<DataWord, DataWord> {
        throw RuntimeException("Not supported")
    }

    override fun setStorage(storageKeys: List<DataWord>, storageValues: List<DataWord>) {
        throw RuntimeException("Not supported")
    }

    override fun setStorage(storage: Map<DataWord, DataWord>) {
        throw RuntimeException("Not supported")
    }

    override fun getAddress(): ByteArray {
        return address
    }

    override fun setAddress(address: ByteArray) {
        throw RuntimeException("Not supported")
    }

    override fun clone(): ContractDetails {
        throw RuntimeException("Not supported")
    }

    override fun syncStorage() {
        throw RuntimeException("Not supported")
    }

    override fun getSnapshotTo(hash: ByteArray): ContractDetails {
        throw RuntimeException("Not supported")
    }
    override fun toString(): String {
        throw RuntimeException("Not supported")
    }
}