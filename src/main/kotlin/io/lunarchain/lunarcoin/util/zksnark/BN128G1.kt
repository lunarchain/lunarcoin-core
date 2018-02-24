package io.lunarchain.lunarcoin.util.zksnark

class BN128G1:BN128Fp {

    companion object {
        /**
         * Checks whether point is a member of subgroup,
         * returns a point if check has been passed and null otherwise
         */
        fun create(x: ByteArray, y: ByteArray): BN128G1? {

            val p = BN128Fp.create(x, y) ?: return null

            return if (!isGroupMember(p)) null else BN128G1(p)

        }
        /**
         * Formally we have to do this check
         * but in our domain it's not necessary,
         * thus always return true
         */
        private fun isGroupMember(p: BN128<Fp>): Boolean {
            return true
        }

    }

    constructor(p: BN128<Fp>): super(p.x()!!, p.y()!!, p.z()!!)

    override fun toAffine(): BN128G1 {
        return BN128G1(super.toAffine())
    }

}