package io.lunarchain.lunarcoin.util.zksnark

import java.math.BigInteger

class Fp6(val a: Fp2, val b: Fp2, val c: Fp2): Field<Fp6> {

    companion object {
        val ZERO = Fp6(Fp2.ZERO, Fp2.ZERO, Fp2.ZERO)
        val _1 = Fp6(Fp2._1, Fp2.ZERO, Fp2.ZERO)
        val NON_RESIDUE = Fp2(BigInteger.valueOf(9), BigInteger.ONE)
        val FROBENIUS_COEFFS_B = arrayOf(

            Fp2(
                BigInteger.ONE,
                BigInteger.ZERO
            ),

            Fp2(
                BigInteger("21575463638280843010398324269430826099269044274347216827212613867836435027261"),
                BigInteger("10307601595873709700152284273816112264069230130616436755625194854815875713954")
            ),

            Fp2(
                BigInteger("21888242871839275220042445260109153167277707414472061641714758635765020556616"),
                BigInteger.ZERO
            ),

            Fp2(
                BigInteger("3772000881919853776433695186713858239009073593817195771773381919316419345261"),
                BigInteger("2236595495967245188281701248203181795121068902605861227855261137820944008926")
            ),

            Fp2(
                BigInteger("2203960485148121921418603742825762020974279258880205651966"),
                BigInteger.ZERO
            ),

            Fp2(
                BigInteger("18429021223477853657660792034369865839114504446431234726392080002137598044644"),
                BigInteger("9344045779998320333812420223237981029506012124075525679208581902008406485703")
            )
        )

        val FROBENIUS_COEFFS_C = arrayOf(

            Fp2(
                BigInteger.ONE,
                BigInteger.ZERO
            ),

            Fp2(
                BigInteger("2581911344467009335267311115468803099551665605076196740867805258568234346338"),
                BigInteger("19937756971775647987995932169929341994314640652964949448313374472400716661030")
            ),

            Fp2(
                BigInteger("2203960485148121921418603742825762020974279258880205651966"),
                BigInteger.ZERO
            ),

            Fp2(
                BigInteger("5324479202449903542726783395506214481928257762400643279780343368557297135718"),
                BigInteger("16208900380737693084919495127334387981393726419856888799917914180988844123039")
            ),

            Fp2(
                BigInteger("21888242871839275220042445260109153167277707414472061641714758635765020556616"),
                BigInteger.ZERO
            ),

            Fp2(
                BigInteger("13981852324922362344252311234282257507216387789820983642040889267519694726527"),
                BigInteger("7629828391165209371577384193250820201684255241773809077146787135900891633097")
            )
        )
    }

    override fun squared(): Fp6 {

        val s0 = a.squared()
        val ab = a.mul(b)
        val s1 = ab.dbl()
        val s2 = a.sub(b).add(c).squared()
        val bc = b.mul(c)
        val s3 = bc.dbl()
        val s4 = c.squared()

        val ra = s0.add(s3.mulByNonResidue())
        val rb = s1.add(s4.mulByNonResidue())
        val rc = s1.add(s2).add(s3).sub(s0).sub(s4)

        return Fp6(ra, rb, rc)
    }

    override fun dbl(): Fp6 {
        return this.add(this)
    }

    override fun mul(o: Fp6?): Fp6 {

        val a1 = a
        val b1 = b
        val c1 = c
        val a2 = o!!.a
        val b2 = o.b
        val c2 = o.c

        val a1a2 = a1.mul(a2)
        val b1b2 = b1.mul(b2)
        val c1c2 = c1.mul(c2)

        val ra = a1a2.add(b1.add(c1).mul(b2.add(c2)).sub(b1b2).sub(c1c2).mulByNonResidue())
        val rb = a1.add(b1).mul(a2.add(b2)).sub(a1a2).sub(b1b2).add(c1c2.mulByNonResidue())
        val rc = a1.add(c1).mul(a2.add(c2)).sub(a1a2).add(b1b2).sub(c1c2)

        return Fp6(ra, rb, rc)
    }

    fun mul(o: Fp2): Fp6 {

        val ra = a.mul(o)
        val rb = b.mul(o)
        val rc = c.mul(o)

        return Fp6(ra, rb, rc)
    }

    fun mulByNonResidue(): Fp6 {

        val ra = NON_RESIDUE.mul(c)
        val rb = a
        val rc = b

        return Fp6(ra, rb, rc)
    }

    override fun add(o: Fp6?): Fp6 {

        val ra = a.add(o!!.a)
        val rb = b.add(o.b)
        val rc = c.add(o.c)

        return Fp6(ra, rb, rc)
    }

    override fun sub(o: Fp6?): Fp6 {

        val ra = a.sub(o!!.a)
        val rb = b.sub(o.b)
        val rc = c.sub(o.c)

        return Fp6(ra, rb, rc)
    }

    override fun inverse(): Fp6 {

        /* From "High-Speed Software Implementation of the Optimal Ate Pairing over Barreto-Naehrig Curves"; Algorithm 17 */

        val t0 = a.squared()
        val t1 = b.squared()
        val t2 = c.squared()
        val t3 = a.mul(b)
        val t4 = a.mul(c)
        val t5 = b.mul(c)
        val c0 = t0.sub(t5.mulByNonResidue())
        val c1 = t2.mulByNonResidue().sub(t3)
        val c2 = t1.sub(t4) // typo in paper referenced above. should be "-" as per Scott, but is "*"
        val t6 = a.mul(c0).add(c.mul(c1).add(b.mul(c2)).mulByNonResidue()).inverse()

        val ra = t6.mul(c0)
        val rb = t6.mul(c1)
        val rc = t6.mul(c2)

        return Fp6(ra, rb, rc)
    }

    override fun negate(): Fp6 {
        return Fp6(a.negate(), b.negate(), c.negate())
    }

    override fun isZero(): Boolean {
        return this == ZERO
    }

    override fun isValid(): Boolean {
        return a.isValid() && b.isValid() && c.isValid()
    }

    fun frobeniusMap(power: Int): Fp6 {

        val ra = a.frobeniusMap(power)
        val rb = FROBENIUS_COEFFS_B[power % 6].mul(b.frobeniusMap(power))
        val rc = FROBENIUS_COEFFS_C[power % 6].mul(c.frobeniusMap(power))

        return Fp6(ra, rb, rc)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is Fp6) return false

        val fp6 = o as Fp6?

        if (if (a != null) !a.equals(fp6!!.a) else fp6!!.a != null) return false
        return if (if (b != null) !b.equals(fp6.b) else fp6.b != null) false else !if (c != null) !c.equals(fp6.c) else fp6.c != null
    }

}