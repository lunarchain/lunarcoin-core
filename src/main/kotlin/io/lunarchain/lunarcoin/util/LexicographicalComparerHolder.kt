package io.lunarchain.lunarcoin.util

object LexicographicalComparerHolder{
    internal val UNSAFE_COMPARER_NAME = LexicographicalComparerHolder::class.java.name + "\$UnsafeComparer"

    internal val BEST_COMPARER = getBestComparer()

    internal fun getBestComparer(): FastByteComparisons.Comparer<ByteArray> {
        try {
            val theClass = Class.forName(UNSAFE_COMPARER_NAME)

            // yes, UnsafeComparer does implement Comparer<byte[]>
            return theClass.enumConstants[0] as FastByteComparisons.Comparer<ByteArray>
        } catch (t: Throwable) { // ensure we really catch *everything*
            return FastByteComparisons.lexicographicalComparerJavaImpl()
        }
    }

    internal enum class PureJavaComparer : FastByteComparisons.Comparer<ByteArray> {
        INSTANCE;

        override fun compareTo(buffer1: ByteArray, offset1: Int, length1: Int,
                               buffer2: ByteArray, offset2: Int, length2: Int): Int {
            // Short circuit equal case
            if (buffer1 == buffer2 &&
                    offset1 == offset2 &&
                    length1 == length2) {
                return 0
            }
            val end1 = offset1 + length1
            val end2 = offset2 + length2
            var i = offset1
            var j = offset2
            while (i < end1 && j < end2) {
                val a = buffer1[i].toInt() and 0xff
                val b = buffer2[j].toInt() and 0xff
                if (a != b) {
                    return a - b
                }
                i++
                j++
            }
            return length1 - length2
        }
    }

}