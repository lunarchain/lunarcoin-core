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
}