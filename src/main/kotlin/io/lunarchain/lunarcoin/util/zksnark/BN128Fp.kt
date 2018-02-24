package io.lunarchain.lunarcoin.util.zksnark

import io.lunarchain.lunarcoin.util.zksnark.Params.B_Fp

open class BN128Fp: BN128<Fp> {


    constructor(x: Fp, y: Fp, z: Fp): super(x,y,z)

    companion object {
        val ZERO: BN128<Fp> = BN128Fp(Fp.ZERO, Fp.ZERO, Fp.ZERO)
        /**
         * Checks whether x and y belong to Fp,
         * then checks whether point with (x; y) coordinates lays on the curve.
         *
         * Returns new point if all checks have been passed,
         * otherwise returns null
         */
        fun create(xx: ByteArray, yy: ByteArray): BN128<Fp>? {

            val x = Fp.create(xx)
            val y = Fp.create(yy)

            // check for point at infinity
            if (x.isZero() && y.isZero()) {
                return ZERO
            }

            val p = BN128Fp(x, y, Fp._1)

            // check whether point is a valid one
            return if (p.isValid()) {
                p
            } else {
                null
            }
        }
    }

    override fun zero(): BN128<Fp> {
        return ZERO
    }

    override fun instance(x: Fp?, y: Fp?, z: Fp?): BN128<Fp> {
        return BN128Fp(x!!, y!!, z!!)
    }

    override fun b(): Fp {
        return B_Fp
    }

    override fun one(): Fp {
        return Fp._1
    }
}


