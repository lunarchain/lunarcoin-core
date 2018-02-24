package io.lunarchain.lunarcoin.util.zksnark

interface Field<T> {

    fun isValid(): Boolean
    fun isZero(): Boolean
    fun add(o: T?): T
    fun mul(o: T?): T
    fun sub(o: T?): T
    fun squared(): T
    fun dbl(): T?
    fun inverse(): T
    fun negate(): T
}