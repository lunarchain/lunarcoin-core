package io.lunarchain.lunarcoin.util


object FastByteComparisons {

    fun equal(b1: ByteArray, b2: ByteArray): Boolean {
        return b1.size == b2.size && compareTo(b1, 0, b1.size, b2, 0, b2.size) == 0
    }

    fun compareTo(b1: ByteArray, s1: Int, l1: Int, b2: ByteArray, s2: Int, l2: Int): Int {
        return LexicographicalComparerHolder.BEST_COMPARER.compareTo(
                b1, s1, l1, b2, s2, l2)
    }

    interface Comparer<T> {
        fun compareTo(buffer1: T, offset1: Int, length1: Int,
                      buffer2: T, offset2: Int, length2: Int): Int
    }

    fun lexicographicalComparerJavaImpl(): Comparer<ByteArray> {
        return LexicographicalComparerHolder.PureJavaComparer.INSTANCE
    }

}