package lunar.vm

import io.lunarchain.lunarcoin.util.ByteUtil
import io.lunarchain.lunarcoin.util.FastByteComparisons
import org.spongycastle.util.Arrays
import java.math.BigInteger
import java.nio.ByteBuffer


class DataWord(): Comparable<DataWord> {

    private var data: ByteArray = ByteArray(32)
    companion object {
        val _2_256: BigInteger = BigInteger.valueOf(2).pow(256)
        val ZERO = DataWord(ByteArray(32))      // don't push it in to the stack
        val MAX_VALUE: BigInteger = _2_256.subtract(BigInteger.ONE)
    }

    constructor(data: ByteArray?): this() {
        if(data == null) return
        if (data.size <= 32)
            System.arraycopy(data, 0, this.data, 32 - data.size, data.size)
        else
            throw RuntimeException("Data word can't exceed 32 bytes: " + data)
    }


    constructor(buffer: ByteBuffer): this(){
        val data = ByteBuffer.allocate(32)
        val array = buffer.array()
        System.arraycopy(array, 0, data.array(), 32 - array.size, array.size)
        this.data = data.array()
    }

    constructor(num: Int): this(ByteBuffer.allocate(4).putInt(num))

    constructor(num: Long): this(ByteBuffer.allocate(8).putLong(num))


    fun getData(): ByteArray {
        return this.data
    }

    fun isZero(): Boolean {
        for (tmp in data) {
            if (tmp.toInt() != 0) return false
        }
        return true
    }

    override fun compareTo(other: DataWord): Int {
        val result = FastByteComparisons.compareTo(
                data, 0, data.size,
                other.getData(), 0, other.getData().size)
        // Convert result into -1, 0 or 1 as is the convention
        return Math.signum(result.toFloat()).toInt()
    }

    /**
     * Converts this DataWord to an int, checking for lost information.
     * If this DataWord is out of the possible range for an int result
     * then an ArithmeticException is thrown.
     *
     * @return this DataWord converted to an int.
     * @throws ArithmeticException - if this will not fit in an int.
     */
    fun intValue(): Int {
        var intVal = 0

        for (aData in data) {
            intVal = (intVal shl 8) + (aData.toInt() and 0xff)
        }

        return intVal
    }

    fun value(): BigInteger {
        return BigInteger(1, data)
    }

    fun getLast20Bytes(): ByteArray {
        return Arrays.copyOfRange(data, 12, data.size)
    }

    fun clone(): DataWord {
        return DataWord(Arrays.clone(data))
    }

    /**
     * In case of long overflow returns Long.MAX_VALUE
     * otherwise works as #longValue()
     */
    fun longValueSafe(): Long {
        val bytesOccupied = bytesOccupied()
        val longValue = longValue()
        return if (bytesOccupied > 8 || longValue < 0) java.lang.Long.MAX_VALUE else longValue
    }


    fun bytesOccupied(): Int {
        val firstNonZero = ByteUtil.firstNonZeroByte(data)
        return if (firstNonZero == -1) 0 else 31 - firstNonZero + 1
    }


    /**
     * Converts this DataWord to a long, checking for lost information.
     * If this DataWord is out of the possible range for a long result
     * then an ArithmeticException is thrown.
     *
     * @return this DataWord converted to a long.
     * @throws ArithmeticException - if this will not fit in a long.
     */
    fun longValue(): Long {

        var longVal: Long = 0
        for (aData in data) {
            longVal = (longVal shl 8) + (aData.toInt() and 0xff)
        }

        return longVal
    }

    fun sub(word: DataWord) {
        val result = value().subtract(word.value())
        this.data = ByteUtil.copyToArray(result.and(MAX_VALUE))
    }

    fun and(w2: DataWord): DataWord {

        for (i in this.data.indices) {
            this.data[i] = (this.data[i].toInt() and w2.data[i].toInt()).toByte()
        }
        return this
    }

    operator fun div(word: DataWord) {

        if (word.isZero()) {
            this.and(ZERO)
            return
        }

        val result = value().divide(word.value())
        this.data = ByteUtil.copyToArray(result.and(MAX_VALUE))
    }
}