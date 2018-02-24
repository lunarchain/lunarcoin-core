package io.lunarchain.lunarcoin.util

import io.lunarchain.lunarcoin.util.ByteUtil.EMPTY_BYTE_ARRAY
import io.lunarchain.lunarcoin.util.CryptoUtil.Companion.sha3
import org.spongycastle.crypto.digests.RIPEMD160Digest
import java.security.MessageDigest
import java.util.Arrays.copyOfRange

object HashUtil {
    val EMPTY_DATA_HASH: ByteArray = sha3(EMPTY_BYTE_ARRAY)
    val EMPTY_LIST_HASH: ByteArray = sha3(RLP.encodeList())
    val EMPTY_TRIE_HASH: ByteArray = sha3(RLP.encodeElement(EMPTY_BYTE_ARRAY))
    val sha256digest = MessageDigest.getInstance("SHA-256")

    /**
     * @param input
     * - data for hashing
     * @return - sha256 hash of the data
     */
    fun sha256(input: ByteArray): ByteArray {
        return sha256digest.digest(input)
    }

    /**
     * @param data
     * - message to hash
     * @return - reipmd160 hash of the message
     */
    fun ripemd160(data: ByteArray?): ByteArray {
        val digest = RIPEMD160Digest()
        if (data != null) {
            val resBuf = ByteArray(digest.digestSize)
            digest.update(data, 0, data.size)
            digest.doFinal(resBuf, 0)
            return resBuf
        }
        throw NullPointerException("Can't hash a NULL value")
    }

    /**
     * Calculates RIGTMOST160(SHA3(input)). This is used in address
     * calculations. *
     *
     * @param input
     * - data
     * @return - 20 right bytes of the hash keccak of the data
     */
    fun sha3omit12(input: ByteArray): ByteArray {
        val hash = sha3(input)
        return copyOfRange(hash, 12, hash.size)
    }
}