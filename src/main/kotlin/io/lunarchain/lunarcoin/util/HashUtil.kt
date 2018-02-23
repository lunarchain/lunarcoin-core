package io.lunarchain.lunarcoin.util

import io.lunarchain.lunarcoin.util.ByteUtil.EMPTY_BYTE_ARRAY
import io.lunarchain.lunarcoin.util.CryptoUtil.Companion.sha3

object HashUtil {
    val EMPTY_DATA_HASH: ByteArray = sha3(EMPTY_BYTE_ARRAY)
    val EMPTY_LIST_HASH: ByteArray = sha3(RLP.encodeList())
    val EMPTY_TRIE_HASH: ByteArray = sha3(RLP.encodeElement(EMPTY_BYTE_ARRAY))
}