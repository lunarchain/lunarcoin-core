package io.lunarchain.lunarcoin.util.zksnark

import java.math.BigInteger

class Fp12(val a: Fp6, val b: Fp6): Field<Fp12> {

    companion object {
        val ZERO = Fp12(Fp6.ZERO, Fp6.ZERO)
        val _1 = Fp12(Fp6._1, Fp6.ZERO)

        val FROBENIUS_COEFFS_B = arrayOf(

            Fp2(
                BigInteger.ONE,
                BigInteger.ZERO
            ),

            Fp2(
                BigInteger("8376118865763821496583973867626364092589906065868298776909617916018768340080"),
                BigInteger("16469823323077808223889137241176536799009286646108169935659301613961712198316")
            ),

            Fp2(
                BigInteger("21888242871839275220042445260109153167277707414472061641714758635765020556617"),
                BigInteger.ZERO
            ),

            Fp2(
                BigInteger("11697423496358154304825782922584725312912383441159505038794027105778954184319"),
                BigInteger("303847389135065887422783454877609941456349188919719272345083954437860409601")
            ),

            Fp2(
                BigInteger("21888242871839275220042445260109153167277707414472061641714758635765020556616"),
                BigInteger.ZERO
            ),

            Fp2(
                BigInteger("3321304630594332808241809054958361220322477375291206261884409189760185844239"),
                BigInteger("5722266937896532885780051958958348231143373700109372999374820235121374419868")
            ),

            Fp2(
                BigInteger("21888242871839275222246405745257275088696311157297823662689037894645226208582"),
                BigInteger.ZERO
            ),

            Fp2(
                BigInteger("13512124006075453725662431877630910996106405091429524885779419978626457868503"),
                BigInteger("5418419548761466998357268504080738289687024511189653727029736280683514010267")
            ),

            Fp2(
                BigInteger("2203960485148121921418603742825762020974279258880205651966"),
                BigInteger.ZERO
            ),

            Fp2(
                BigInteger("10190819375481120917420622822672549775783927716138318623895010788866272024264"),
                BigInteger("21584395482704209334823622290379665147239961968378104390343953940207365798982")
            ),

            Fp2(
                BigInteger("2203960485148121921418603742825762020974279258880205651967"),
                BigInteger.ZERO
            ),

            Fp2(
                BigInteger("18566938241244942414004596690298913868373833782006617400804628704885040364344"),
                BigInteger("16165975933942742336466353786298926857552937457188450663314217659523851788715")
            )
        )
    }

    override fun squared(): Fp12 {

        val ab = a.mul(b)

        val ra = a.add(b).mul(a.add(b.mulByNonResidue())).sub(ab).sub(ab.mulByNonResidue())
        val rb = ab.add(ab)

        return Fp12(ra, rb)
    }

    override fun dbl(): Fp12? {
        return null
    }

    fun mulBy024(ell0: Fp2, ellVW: Fp2, ellVV: Fp2): Fp12 {

        var z0 = a.a
        var z1 = a.b
        var z2 = a.c
        var z3 = b.a
        var z4 = b.b
        var z5 = b.c

        var t0: Fp2
        var t1: Fp2
        val t2: Fp2
        val s0: Fp2
        var t3: Fp2
        var t4: Fp2
        val d0: Fp2
        val d2: Fp2
        val d4: Fp2
        var s1: Fp2

        d0 = z0.mul(ell0)
        d2 = z2.mul(ellVV)
        d4 = z4.mul(ellVW)
        t2 = z0.add(z4)
        t1 = z0.add(z2)
        s0 = z1.add(z3).add(z5)

        // For z.a_.a_ = z0.
        s1 = z1.mul(ellVV)
        t3 = s1.add(d4)
        t4 = Fp6.NON_RESIDUE.mul(t3).add(d0)
        z0 = t4

        // For z.a_.b_ = z1
        t3 = z5.mul(ellVW)
        s1 = s1.add(t3)
        t3 = t3.add(d2)
        t4 = Fp6.NON_RESIDUE.mul(t3)
        t3 = z1.mul(ell0)
        s1 = s1.add(t3)
        t4 = t4.add(t3)
        z1 = t4

        // For z.a_.c_ = z2
        t0 = ell0.add(ellVV)
        t3 = t1.mul(t0).sub(d0).sub(d2)
        t4 = z3.mul(ellVW)
        s1 = s1.add(t4)
        t3 = t3.add(t4)

        // For z.b_.a_ = z3 (z3 needs z2)
        t0 = z2.add(z4)
        z2 = t3
        t1 = ellVV.add(ellVW)
        t3 = t0.mul(t1).sub(d2).sub(d4)
        t4 = Fp6.NON_RESIDUE.mul(t3)
        t3 = z3.mul(ell0)
        s1 = s1.add(t3)
        t4 = t4.add(t3)
        z3 = t4

        // For z.b_.b_ = z4
        t3 = z5.mul(ellVV)
        s1 = s1.add(t3)
        t4 = Fp6.NON_RESIDUE.mul(t3)
        t0 = ell0.add(ellVW)
        t3 = t2.mul(t0).sub(d0).sub(d4)
        t4 = t4.add(t3)
        z4 = t4

        // For z.b_.c_ = z5.
        t0 = ell0.add(ellVV).add(ellVW)
        t3 = s0.mul(t0).sub(s1)
        z5 = t3

        return Fp12(Fp6(z0, z1, z2), Fp6(z3, z4, z5))
    }

    override fun add(o: Fp12?): Fp12 {
        return Fp12(a.add(o!!.a), b.add(o.b))
    }

    override fun mul(o: Fp12?): Fp12 {

        val a2 = o!!.a
        val b2 = o.b
        val a1 = a
        val b1 = b

        val a1a2 = a1.mul(a2)
        val b1b2 = b1.mul(b2)

        val ra = a1a2.add(b1b2.mulByNonResidue())
        val rb = a1.add(b1).mul(a2.add(b2)).sub(a1a2).sub(b1b2)

        return Fp12(ra, rb)
    }

    override fun sub(o: Fp12?): Fp12 {
        return Fp12(a.sub(o!!.a), b.sub(o.b))
    }

    override fun inverse(): Fp12 {

        val t0 = a.squared()
        val t1 = b.squared()
        val t2 = t0.sub(t1.mulByNonResidue())
        val t3 = t2.inverse()

        val ra = a.mul(t3)
        val rb = b.mul(t3).negate()

        return Fp12(ra, rb)
    }

    override fun negate(): Fp12 {
        return Fp12(a.negate(), b.negate())
    }

    override fun isZero(): Boolean {
        return this == ZERO
    }

    override fun isValid(): Boolean {
        return a.isValid() && b.isValid()
    }

    fun frobeniusMap(power: Int): Fp12 {

        val ra = a.frobeniusMap(power)
        val rb = b.frobeniusMap(power).mul(FROBENIUS_COEFFS_B[power % 12])

        return Fp12(ra, rb)
    }

    fun cyclotomicSquared(): Fp12 {

        var z0 = a.a
        var z4 = a.b
        var z3 = a.c
        var z2 = b.a
        var z1 = b.b
        var z5 = b.c

        val t0: Fp2
        val t1: Fp2
        val t2: Fp2
        val t3: Fp2
        val t4: Fp2
        val t5: Fp2
        var tmp: Fp2

        // t0 + t1*y = (z0 + z1*y)^2 = a^2
        tmp = z0.mul(z1)
        t0 = z0.add(z1).mul(z0.add(Fp6.NON_RESIDUE.mul(z1))).sub(tmp).sub(Fp6.NON_RESIDUE.mul(tmp))
        t1 = tmp.add(tmp)
        // t2 + t3*y = (z2 + z3*y)^2 = b^2
        tmp = z2.mul(z3)
        t2 = z2.add(z3).mul(z2.add(Fp6.NON_RESIDUE.mul(z3))).sub(tmp).sub(Fp6.NON_RESIDUE.mul(tmp))
        t3 = tmp.add(tmp)
        // t4 + t5*y = (z4 + z5*y)^2 = c^2
        tmp = z4.mul(z5)
        t4 = z4.add(z5).mul(z4.add(Fp6.NON_RESIDUE.mul(z5))).sub(tmp).sub(Fp6.NON_RESIDUE.mul(tmp))
        t5 = tmp.add(tmp)

        // for A

        // z0 = 3 * t0 - 2 * z0
        z0 = t0.sub(z0)
        z0 = z0.add(z0)
        z0 = z0.add(t0)
        // z1 = 3 * t1 + 2 * z1
        z1 = t1.add(z1)
        z1 = z1.add(z1)
        z1 = z1.add(t1)

        // for B

        // z2 = 3 * (xi * t5) + 2 * z2
        tmp = Fp6.NON_RESIDUE.mul(t5)
        z2 = tmp.add(z2)
        z2 = z2.add(z2)
        z2 = z2.add(tmp)

        // z3 = 3 * t4 - 2 * z3
        z3 = t4.sub(z3)
        z3 = z3.add(z3)
        z3 = z3.add(t4)

        // for C

        // z4 = 3 * t2 - 2 * z4
        z4 = t2.sub(z4)
        z4 = z4.add(z4)
        z4 = z4.add(t2)

        // z5 = 3 * t3 + 2 * z5
        z5 = t3.add(z5)
        z5 = z5.add(z5)
        z5 = z5.add(t3)

        return Fp12(Fp6(z0, z4, z3), Fp6(z2, z1, z5))
    }

    fun cyclotomicExp(pow: BigInteger): Fp12 {

        var res = _1

        for (i in pow.bitLength() - 1 downTo 0) {
            res = res.cyclotomicSquared()

            if (pow.testBit(i)) {
                res = res.mul(this)
            }
        }

        return res
    }

    fun unitaryInverse(): Fp12 {

        val ra = a
        val rb = b.negate()

        return Fp12(ra, rb)
    }

    fun negExp(exp: BigInteger): Fp12 {
        return this.cyclotomicExp(exp).unitaryInverse()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is Fp12) return false

        val fp12 = o as Fp12?

        return if (if (a != null) !a.equals(fp12!!.a) else fp12!!.a != null) false else !if (b != null) !b.equals(fp12.b) else fp12.b != null

    }

    override fun toString():String {
return String.format(
    "Fp12 (%s; %s)\n" +
    "     (%s; %s)\n" +
    "     (%s; %s)\n" +
    "     (%s; %s)\n" +
    "     (%s; %s)\n" +
    "     (%s; %s)\n",

a.a.a, a.a.b,
a.b.a, a.b.b,
a.c.a, a.c.b,
b.a.a, b.a.b,
b.b.a, b.b.b,
b.c.a, b.c.b
)
}




}