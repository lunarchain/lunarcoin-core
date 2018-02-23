package io.lunarchain.lunarcoin.util

import org.spongycastle.util.encoders.Hex
import java.io.Serializable
import java.util.*

class ByteArrayWrapper(private val data: ByteArray): Comparable<ByteArrayWrapper>, Serializable {
    private var hashCode = Arrays.hashCode(data)



    override fun equals(other: Any?): Boolean {
        if (other !is ByteArrayWrapper)
            return false
        val otherData = other.getData()
        return FastByteComparisons.compareTo(
                data, 0, data.size,
                otherData, 0, otherData.size) === 0
    }

    override fun hashCode(): Int {
        return hashCode
    }

    override fun compareTo(o: ByteArrayWrapper): Int {
        return FastByteComparisons.compareTo(
                data, 0, data.size,
                o.getData(), 0, o.getData().size)
    }

    fun getData(): ByteArray {
        return data
    }

    override fun toString(): String {
        return Hex.toHexString(data)
    }
}