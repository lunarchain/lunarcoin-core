package io.lunarchain.lunarcoin.util.zksnark

import io.lunarchain.lunarcoin.util.zksnark.Params.B_Fp2
import java.math.BigInteger

open class BN128Fp2: BN128<Fp2> {

    companion object {
        internal val ZERO: BN128<Fp2> = BN128Fp2(Fp2.ZERO, Fp2.ZERO, Fp2.ZERO)

        /**
         * Checks whether provided data are coordinates of a point on the curve,
         * then checks if this point is a member of subgroup of order "r"
         * and if checks have been passed it returns a point, otherwise returns null
         */
        fun create(aa: ByteArray, bb: ByteArray, cc: ByteArray, dd: ByteArray): BN128<Fp2>? {

            val x = Fp2.create(aa, bb)
            val y = Fp2.create(cc, dd)

            // check for point at infinity
            if (x.isZero() && y.isZero()) {
                return ZERO
            }

            val p = BN128Fp2(x, y, Fp2._1)

            // check whether point is a valid one
            return if (p.isValid()) {
                p
            } else {
                null
            }
        }
    }

    constructor(x: Fp2, y: Fp2, z: Fp2): super(x, y, z)
    constructor(a: BigInteger, b: BigInteger, c: BigInteger, d: BigInteger): super(Fp2.create(a, b), Fp2.create(c, d), Fp2._1)

    override fun zero(): BN128<Fp2> {
        return ZERO
    }

    override fun instance(x: Fp2?, y: Fp2?, z: Fp2?): BN128<Fp2> {
        return BN128Fp2(x!!, y!!, z!!)
    }

    override fun b(): Fp2 {
        return B_Fp2
    }

    override fun one(): Fp2 {
        return Fp2._1
    }

}