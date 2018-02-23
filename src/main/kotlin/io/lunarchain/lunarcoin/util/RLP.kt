package io.lunarchain.lunarcoin.util

import io.lunarchain.lunarcoin.util.ByteUtil.isNullOrZeroArray
import io.lunarchain.lunarcoin.util.ByteUtil.isSingleZero
import java.util.*

object RLP {


    /**
     * [0x80]
     * If a string is 0-55 bytes long, the RLP encoding consists of a single
     * byte with value 0x80 plus the length of the string followed by the
     * string. The range of the first byte is thus [0x80, 0xb7].
     */
    private const val OFFSET_SHORT_ITEM = 0x80

    /**
     * [0xb7]
     * If a string is more than 55 bytes long, the RLP encoding consists of a
     * single byte with value 0xb7 plus the length of the length of the string
     * in binary form, followed by the length of the string, followed by the
     * string. For example, a length-1024 string would be encoded as
     * \xb9\x04\x00 followed by the string. The range of the first byte is thus
     * [0xb8, 0xbf].
     */
    private const val OFFSET_LONG_ITEM = 0xb7

    /**
     * Reason for threshold according to Vitalik Buterin:
     * - 56 bytes maximizes the benefit of both options
     * - if we went with 60 then we would have only had 4 slots for long strings
     * so RLP would not have been able to store objects above 4gb
     * - if we went with 48 then RLP would be fine for 2^128 space, but that's way too much
     * - so 56 and 2^64 space seems like the right place to put the cutoff
     * - also, that's where Bitcoin's varint does the cutof
     */
    private const val SIZE_THRESHOLD = 56


    /**
     * [0xc0]
     * If the total payload of a list (i.e. the combined length of all its
     * items) is 0-55 bytes long, the RLP encoding consists of a single byte
     * with value 0xc0 plus the length of the list followed by the concatenation
     * of the RLP encodings of the items. The range of the first byte is thus
     * [0xc0, 0xf7].
     */
    private const val OFFSET_SHORT_LIST = 0xc0

    /**
     * [0xf7]
     * If the total payload of a list is more than 55 bytes long, the RLP
     * encoding consists of a single byte with value 0xf7 plus the length of the
     * length of the list in binary form, followed by the length of the list,
     * followed by the concatenation of the RLP encodings of the items. The
     * range of the first byte is thus [0xf8, 0xff].
     */
    private const val OFFSET_LONG_LIST = 0xf7


    fun encodeList(vararg elements: ByteArray): ByteArray {

        if (elements == null) {
            return byteArrayOf(OFFSET_SHORT_LIST.toByte())
        }

        var totalLength = 0
        for (element1 in elements) {
            totalLength += element1.size
        }

        val data: ByteArray
        var copyPos: Int
        if (totalLength < SIZE_THRESHOLD) {

            data = ByteArray(1 + totalLength)
            data[0] = (OFFSET_SHORT_LIST + totalLength).toByte()
            copyPos = 1
        } else {
            // length of length = BX
            // prefix = [BX, [length]]
            var tmpLength = totalLength
            var byteNum: Byte = 0
            while (tmpLength != 0) {
                ++byteNum
                tmpLength = tmpLength shr 8
            }
            tmpLength = totalLength
            val lenBytes = ByteArray(byteNum.toInt())
            for (i in 0 until byteNum) {
                lenBytes[byteNum.toInt() - 1 - i] = (tmpLength shr 8 * i and 0xFF).toByte()
            }
            // first byte = F7 + bytes.length
            data = ByteArray(1 + lenBytes.size + totalLength)
            data[0] = (OFFSET_LONG_LIST + byteNum).toByte()
            System.arraycopy(lenBytes, 0, data, 1, lenBytes.size)

            copyPos = lenBytes.size + 1
        }
        for (element in elements) {
            System.arraycopy(element, 0, data, copyPos, element.size)
            copyPos += element.size
        }
        return data
    }



    fun encodeElement(srcData: ByteArray): ByteArray {

        // [0x80]
        if (isNullOrZeroArray(srcData)) {
            return byteArrayOf(OFFSET_SHORT_ITEM.toByte())

            // [0x00]
        } else if (isSingleZero(srcData)) {
            return srcData

            // [0x01, 0x7f] - single byte, that byte is its own RLP encoding
        } else if (srcData.size == 1 && (srcData[0].toInt() and 0xFF).toByte() < 0x80) {
            return srcData

            // [0x80, 0xb7], 0 - 55 bytes
        } else if (srcData.size < SIZE_THRESHOLD) {
            // length = 8X
            val length = (OFFSET_SHORT_ITEM + srcData.size).toByte()
            val data = Arrays.copyOf(srcData, srcData.size + 1)
            System.arraycopy(data, 0, data, 1, srcData.size)
            data[0] = length

            return data
            // [0xb8, 0xbf], 56+ bytes
        } else {
            // length of length = BX
            // prefix = [BX, [length]]
            var tmpLength = srcData.size
            var lengthOfLength: Byte = 0
            while (tmpLength != 0) {
                ++lengthOfLength
                tmpLength = tmpLength shr 8
            }

            // set length Of length at first byte
            val data = ByteArray(1 + lengthOfLength.toInt() + srcData.size)
            data[0] = (OFFSET_LONG_ITEM + lengthOfLength).toByte()

            // copy length after first byte
            tmpLength = srcData.size
            for (i in lengthOfLength downTo 1) {
                data[i] = (tmpLength and 0xFF).toByte()
                tmpLength = tmpLength shr 8
            }

            // at last copy the number bytes after its length
            System.arraycopy(srcData, 0, data, 1 + lengthOfLength, srcData.size)

            return data
        }
    }
}