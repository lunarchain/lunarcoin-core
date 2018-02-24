package io.lunarchain.lunarcoin.util

import org.spongycastle.util.encoders.Hex
import java.math.BigInteger
import java.nio.ByteBuffer

object ByteUtil {
    val EMPTY_BYTE_ARRAY = ByteArray(0)
    val ZERO_BYTE_ARRAY = byteArrayOf(0)
    fun oneByteToHexString(value: Byte): String {
        var retVal = Integer.toString(value.toInt() and 0xFF, 16)
        if (retVal.length == 1) retVal = "0" + retVal
        return retVal
    }

    fun isNullOrZeroArray(array: ByteArray?): Boolean {
        return array == null || array.size == 0
    }

    fun isSingleZero(array: ByteArray): Boolean {
        return array.size == 1 && array[0].toInt() == 0
    }

    /**
     * Convert a byte-array into a hex String.<br></br>
     * Works similar to [Hex.toHexString]
     * but allows for `null`
     *
     * @param data - byte-array to convert to a hex-string
     * @return hex representation of the data.<br></br>
     * Returns an empty String if the input is `null`
     *
     * @see Hex.toHexString
     */
    fun toHexString(data: ByteArray?): String {
        return if (data == null) "" else Hex.toHexString(data)
    }

    fun firstNonZeroByte(data: ByteArray): Int {
        for (i in data.indices) {
            if (data[i].toInt() != 0) {
                return i
            }
        }
        return -1
    }

    /**
     * Omitting sign indication byte.
     * <br></br><br></br>
     * Instead of [org.spongycastle.util.BigIntegers.asUnsignedByteArray]
     * <br></br>we use this custom method to avoid an empty array in case of BigInteger.ZERO
     *
     * @param value - any big integer number. A `null`-value will return `null`
     * @return A byte array without a leading zero byte if present in the signed encoding.
     * BigInteger.ZERO will return an array with length 1 and byte-value 0.
     */
    fun bigIntegerToBytes(value: BigInteger?): ByteArray? {
        if (value == null)
            return null

        var data = value.toByteArray()

        if (data.size != 1 && data[0].toInt() == 0) {
            val tmp = ByteArray(data.size - 1)
            System.arraycopy(data, 1, tmp, 0, tmp.size)
            data = tmp
        }
        return data
    }

    /**
     * Utility function to copy a byte array into a new byte array with given size.
     * If the src length is smaller than the given size, the result will be left-padded
     * with zeros.
     *
     * @param value - a BigInteger with a maximum value of 2^256-1
     * @return Byte array of given size with a copy of the `src`
     */
    fun copyToArray(value: BigInteger): ByteArray {
        val src = ByteUtil.bigIntegerToBytes(value)
        val dest = ByteBuffer.allocate(32).array()
        System.arraycopy(src, 0, dest, dest.size - src!!.size, src.size)
        return dest
    }

    fun stripLeadingZeroes(data: ByteArray?): ByteArray? {

        if (data == null)
            return null

        val firstNonZero = firstNonZeroByte(data)
        when (firstNonZero) {
            -1 -> return ZERO_BYTE_ARRAY

            0 -> return data

            else -> {
                val result = ByteArray(data.size - firstNonZero)
                System.arraycopy(data, firstNonZero, result, 0, data.size - firstNonZero)

                return result
            }
        }
    }

    /**
     * Parses fixed number of bytes starting from `offset` in `input` array.
     * If `input` has not enough bytes return array will be right padded with zero bytes.
     * I.e. if `offset` is higher than `input.length` then zero byte array of length `len` will be returned
     */
    fun parseBytes(input: ByteArray, offset: Int, len: Int): ByteArray {

        if (offset >= input.size || len == 0)
            return EMPTY_BYTE_ARRAY

        val bytes = ByteArray(len)
        System.arraycopy(input, offset, bytes, 0, Math.min(input.size - offset, len))
        return bytes
    }

    /**
     * Returns a number of zero bits preceding the highest-order ("leftmost") one-bit
     * interpreting input array as a big-endian integer value
     */
    fun numberOfLeadingZeros(bytes: ByteArray): Int {

        val i = firstNonZeroByte(bytes)

        if (i == -1) {
            return bytes.size * 8
        } else {
            val byteLeadingZeros = Integer.numberOfLeadingZeros(bytes[i].toInt() and 0xff) - 24
            return i * 8 + byteLeadingZeros
        }
    }

    /**
     * Cast hex encoded value from byte[] to BigInteger
     * null is parsed like byte[0]
     *
     * @param bb byte array contains the values
     * @return unsigned positive BigInteger value.
     */
    fun bytesToBigInteger(bb: ByteArray?): BigInteger {
        return if (bb == null || bb.size == 0) BigInteger.ZERO else BigInteger(1, bb)
    }

    /**
     * Parses 32-bytes word from given input.
     * Uses [.parseBytes] method,
     * thus, result will be right-padded with zero bytes if there is not enough bytes in `input`
     *
     * @param idx an index of the word starting from `0`
     */
    fun parseWord(input: ByteArray, idx: Int): ByteArray {
        return parseBytes(input, 32 * idx, 32)
    }

    /**
     * Parses 32-bytes word from given input.
     * Uses [.parseBytes] method,
     * thus, result will be right-padded with zero bytes if there is not enough bytes in `input`
     *
     * @param idx an index of the word starting from `0`
     * @param offset an offset in `input` array to start parsing from
     */
    fun parseWord(input: ByteArray, offset: Int, idx: Int): ByteArray {
        return parseBytes(input, offset + 32 * idx, 32)
    }
}