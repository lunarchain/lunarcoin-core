package io.lunarchain.lunarcoin.util.zksnark

import io.lunarchain.lunarcoin.util.zksnark.Params.P
import java.math.BigInteger

class Fp(private val v: BigInteger): Field<Fp> {

    companion object {

        val ZERO = Fp(BigInteger.ZERO)
        val _1 = Fp(BigInteger.ONE)
        val NON_RESIDUE =
            Fp(BigInteger("21888242871839275222246405745257275088696311157297823662689037894645226208582"))

        val _2_INV = Fp(BigInteger.valueOf(2).modInverse(P))
        fun create(v: ByteArray): Fp {
            return Fp(BigInteger(1, v))
        }

        fun create(v: BigInteger): Fp {
            return Fp(v)
        }
    }

    fun bytes(): ByteArray {
        return v!!.toByteArray()
    }

    override fun add(o: Fp?): Fp {
        return Fp(this.v!!.add(o?.v!!).mod(P))
    }

    override fun mul(o: Fp?): Fp {
        return Fp(this.v!!.multiply(o?.v!!).mod(P))
    }

    override fun sub(o: Fp?): Fp {
        return Fp(this.v!!.subtract(o?.v!!).mod(P))
    }

    override fun squared(): Fp {
        return Fp(v!!.multiply(v!!).mod(P))
    }

    override fun dbl(): Fp {
        return Fp(v!!.add(v!!).mod(P))
    }

    override fun inverse(): Fp {
        return Fp(v!!.modInverse(P))
    }

    override fun negate(): Fp {
        return Fp(v!!.negate().mod(P))
    }

    override fun isZero(): Boolean {
        return v!!.compareTo(BigInteger.ZERO) == 0
    }

    /**
     * Checks if provided value is a valid Fp member
     */
    override fun isValid(): Boolean {
        return v!!.compareTo(P) < 0
    }

    fun mul(o: Fp2): Fp2 {
        return Fp2(o.a.mul(this), o.b.mul(this))
    }
    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val fp = o as Fp?

        return !if (v != null) v!!.compareTo(fp!!.v!!) != 0 else fp!!.v != null
    }

    override fun toString(): String {
        return v!!.toString()
    }
}