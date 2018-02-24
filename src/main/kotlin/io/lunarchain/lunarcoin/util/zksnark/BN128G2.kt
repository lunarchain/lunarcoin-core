package io.lunarchain.lunarcoin.util.zksnark

import io.lunarchain.lunarcoin.util.zksnark.Params.R
import io.lunarchain.lunarcoin.util.zksnark.Params.TWIST_MUL_BY_P_X
import io.lunarchain.lunarcoin.util.zksnark.Params.TWIST_MUL_BY_P_Y
import java.math.BigInteger

class BN128G2: BN128Fp2 {

    companion object {

        /**
         * Checks whether provided data are coordinates of a point belonging to subgroup,
         * if check has been passed it returns a point, otherwise returns null
         */
        fun create(a: ByteArray, b: ByteArray, c: ByteArray, d: ByteArray): BN128G2? {

            val p = BN128Fp2.create(a, b, c, d) ?: return null

            // fails if point is invalid

            // check whether point is a subgroup member
            return if (!isGroupMember(p)) null else BN128G2(p)

        }

        private fun isGroupMember(p: BN128<Fp2>): Boolean {
            val left = p.mul(FR_NEG_ONE).add(p)
            return left.isZero() // should satisfy condition: -1 * p + p == 0, where -1 belongs to F_r
        }

        internal val FR_NEG_ONE = BigInteger.ONE.negate().mod(R)
    }

    constructor(p: BN128<Fp2>): super(p.x()!!, p.y()!!, p.z()!!)
    constructor(x: Fp2?, y: Fp2?, z: Fp2?): super(x!!, y!!, z!!)

    override fun toAffine(): BN128G2 {
        return BN128G2(super.toAffine())
    }

    internal fun mulByP(): BN128G2 {

        val rx = TWIST_MUL_BY_P_X.mul(x!!.frobeniusMap(1))
        val ry = TWIST_MUL_BY_P_Y.mul(y!!.frobeniusMap(1))
        val rz = z!!.frobeniusMap(1)

        return BN128G2(rx, ry, rz)
    }
}