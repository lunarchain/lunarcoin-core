package io.lunarchain.lunarcoin.serialization

import io.lunarchain.lunarcoin.core.*
import io.lunarchain.lunarcoin.storage.BlockInfo
import io.lunarchain.lunarcoin.util.CodecUtil
import io.lunarchain.lunarcoin.util.CryptoUtil
import java.io.*


/**
 * 序列化/反序列化接口。
 */
interface Serializer<T, S> {

    /**
     * Converts T ==> S
     * Should correctly handle null parameter
     */
    fun serialize(obj: T): S

    /**
     * Converts S ==> T
     * Should correctly handle null parameter
     */
    fun deserialize(s: S): T?
}

class AccountStateSerialize : Serializer<AccountState, ByteArray> {
    override fun deserialize(s: ByteArray): AccountState? {
        return CodecUtil.decodeAccountState(s)
    }

    override fun serialize(obj: AccountState): ByteArray {
        return CodecUtil.encodeAccountState(obj)
    }

}

class AccountSerialize(val password: String) : Serializer<AccountWithKey, ByteArray> {
    override fun deserialize(s: ByteArray): AccountWithKey? {
        try {
            val privateKey = CryptoUtil.decryptPrivateKey(s, password)
            val publicKey = CryptoUtil.generatePublicKey(privateKey)
            return if (publicKey != null) {
                AccountWithKey(publicKey, privateKey)
            } else {
                null
            }
        } catch (e: Exception) {
            return null
        }
    }

    override fun serialize(obj: AccountWithKey): ByteArray {
        return CryptoUtil.encryptPrivateKey(obj.privateKey, password)
    }

}

class BlockSerialize : Serializer<Block, ByteArray> {
    override fun deserialize(s: ByteArray): Block? {
        return CodecUtil.decodeBlock(s)
    }

    override fun serialize(obj: Block): ByteArray {
        return CodecUtil.encodeBlock(obj)
    }

}

class TransactionSerialize : Serializer<Transaction, ByteArray> {
    override fun deserialize(s: ByteArray): Transaction? {
        return CodecUtil.decodeTransaction(s)
    }

    override fun serialize(obj: Transaction): ByteArray {
        return CodecUtil.encodeTransaction(obj)
    }

}

class BlockInfosSerialize : Serializer<List<BlockInfo>, ByteArray> {
    override fun deserialize(s: ByteArray): List<BlockInfo>? {
        return CodecUtil.decodeBlockInfos(s)
    }

    override fun serialize(obj: List<BlockInfo>): ByteArray {
        return CodecUtil.encodeBlockInfos(obj)
    }

}

class StorageSerialize: Serializer<AccountStorage, ByteArray> {

    override fun serialize(obj: AccountStorage): ByteArray {
        return CodecUtil.encodeAccountStorage(obj)
    }

    override fun deserialize(s: ByteArray): AccountStorage? {
        return CodecUtil.decodeAccountStorage(s)
    }


}

class CodeStorageSerialize: Serializer<CodeStorage, ByteArray> {
    override fun deserialize(s: ByteArray): CodeStorage? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun serialize(obj: CodeStorage): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
