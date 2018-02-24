package io.lunarchain.lunarcoin.util

import io.lunarchain.lunarcoin.storage.Repository
import java.math.BigInteger

object BIUtil {

    /**
     * @param value - not null
     * @return true - if the param is zero
     */
    fun isZero(value: BigInteger): Boolean {
        return value.compareTo(BigInteger.ZERO) == 0
    }

    /**
     * @param valueA - not null
     * @param valueB - not null
     * @return true - if the valueA is equal to valueB is zero
     */
    fun isEqual(valueA: BigInteger, valueB: BigInteger): Boolean {
        return valueA.compareTo(valueB) == 0
    }

    /**
     * @param valueA - not null
     * @param valueB - not null
     * @return true - if the valueA is not equal to valueB is zero
     */
    fun isNotEqual(valueA: BigInteger, valueB: BigInteger): Boolean {
        return !isEqual(valueA, valueB)
    }

    /**
     * @param valueA - not null
     * @param valueB - not null
     * @return true - if the valueA is less than valueB is zero
     */
    fun isLessThan(valueA: BigInteger, valueB: BigInteger): Boolean {
        return valueA.compareTo(valueB) < 0
    }

    /**
     * @param valueA - not null
     * @param valueB - not null
     * @return true - if the valueA is more than valueB is zero
     */
    fun isMoreThan(valueA: BigInteger, valueB: BigInteger): Boolean {
        return valueA.compareTo(valueB) > 0
    }


    /**
     * @param valueA - not null
     * @param valueB - not null
     * @return sum - valueA + valueB
     */
    fun sum(valueA: BigInteger, valueB: BigInteger): BigInteger {
        return valueA.add(valueB)
    }


    /**
     * @param data = not null
     * @return new positive BigInteger
     */
    fun toBI(data: ByteArray): BigInteger {
        return BigInteger(1, data)
    }

    /**
     * @param data = not null
     * @return new positive BigInteger
     */
    fun toBI(data: Long): BigInteger {
        return BigInteger.valueOf(data)
    }


    fun isPositive(value: BigInteger): Boolean {
        return value.signum() > 0
    }

    fun isCovers(covers: BigInteger, value: BigInteger): Boolean {
        return !isNotCovers(covers, value)
    }

    fun isNotCovers(covers: BigInteger, value: BigInteger): Boolean {
        return covers.compareTo(value) < 0
    }


    fun transfer(repository: Repository, fromAddr: ByteArray, toAddr: ByteArray, value: BigInteger) {
        repository.addBalance(fromAddr, value.negate())
        repository.addBalance(toAddr, value)
    }

    fun exitLong(value: BigInteger): Boolean {

        return value.compareTo(BigInteger(java.lang.Long.MAX_VALUE.toString() + "")) > -1
    }

    fun isIn20PercentRange(first: BigInteger, second: BigInteger): Boolean {
        val five = BigInteger.valueOf(5)
        val limit = first.add(first.divide(five))
        return !isMoreThan(second, limit)
    }

    fun max(first: BigInteger, second: BigInteger): BigInteger {
        return if (first.compareTo(second) < 0) second else first
    }

    /**
     * Returns a result of safe addition of two `int` values
     * `Integer.MAX_VALUE` is returned if overflow occurs
     */
    fun addSafely(a: Int, b: Int): Int {
        val res = a.toLong() + b.toLong()
        return if (res > Integer.MAX_VALUE) Integer.MAX_VALUE else res.toInt()
    }
}