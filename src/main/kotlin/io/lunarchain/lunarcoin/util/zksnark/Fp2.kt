package io.lunarchain.lunarcoin.util.zksnark

import java.math.BigInteger

class Fp2(val a: Fp, val b: Fp): Field<Fp2> {
    companion object {
        val ZERO = Fp2(Fp.ZERO, Fp.ZERO)
        val _1 = Fp2(Fp._1, Fp.ZERO)
        val NON_RESIDUE = Fp2(BigInteger.valueOf(9), BigInteger.ONE)
        val FROBENIUS_COEFFS_B = arrayOf(
            Fp(BigInteger.ONE),
            Fp(BigInteger("21888242871839275222246405745257275088696311157297823662689037894645226208582"))
        )

        fun create(aa: BigInteger, bb: BigInteger): Fp2 {

            val a = Fp.create(aa)
            val b = Fp.create(bb)

            return Fp2(a, b)
        }

        fun create(aa: ByteArray, bb: ByteArray): Fp2 {

            val a = Fp.create(aa)
            val b = Fp.create(bb)

            return Fp2(a, b)
        }

    }
    constructor(a: BigInteger, b: BigInteger): this(Fp(a), Fp(b))

    override fun squared(): Fp2 {

        // using Complex squaring

        val ab = a.mul(b)

        val ra = a.add(b).mul(b.mul(Fp.NON_RESIDUE).add(a))
            .sub(ab).sub(ab.mul(Fp.NON_RESIDUE)) // ra = (a + b)(a + NON_RESIDUE * b) - ab - NON_RESIDUE * b
        val rb = ab.dbl()

        return Fp2(ra, rb)
    }

    override fun mul(o: Fp2?): Fp2 {

        val aa = a.mul(o?.a)
        val bb = b.mul(o?.b)

        val ra = bb.mul(Fp.NON_RESIDUE).add(aa)    // ra = a1 * a2 + NON_RESIDUE * b1 * b2
        val rb = a.add(b).mul(o?.a?.add(o.b)).sub(aa).sub(bb)     // rb = (a1 + b1)(a2 + b2) - a1 * a2 - b1 * b2

        return Fp2(ra, rb)
    }

    override fun add(o: Fp2?): Fp2 {
        return Fp2(a.add(o?.a), b.add(o?.b))
    }

    override fun sub(o: Fp2?): Fp2 {
        return Fp2(a.sub(o?.a), b.sub(o?.b))
    }

    override fun dbl(): Fp2 {
        return this.add(this)
    }

    override fun inverse(): Fp2 {

        val t0 = a.squared()
        val t1 = b.squared()
        val t2 = t0.sub(Fp.NON_RESIDUE.mul(t1))
        val t3 = t2.inverse()

        val ra = a.mul(t3)          // ra = a * t3
        val rb = b.mul(t3).negate() // rb = -(b * t3)

        return Fp2(ra, rb)
    }

    override fun negate(): Fp2 {
        return Fp2(a.negate(), b.negate())
    }

    override fun isZero(): Boolean {
        return this == ZERO
    }

    override fun isValid(): Boolean {
        return a.isValid() && b.isValid()
    }

    fun frobeniusMap(power: Int): Fp2 {

        val ra = a
        val rb = FROBENIUS_COEFFS_B[power % 2].mul(b)

        return Fp2(ra, rb)
    }

    fun mulByNonResidue(): Fp2 {
        return NON_RESIDUE.mul(this)
    }
}