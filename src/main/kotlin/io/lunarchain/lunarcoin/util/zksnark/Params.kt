package io.lunarchain.lunarcoin.util.zksnark

import java.math.BigInteger

object Params {
    /**
     * "p" field parameter of F_p, F_p2, F_p6 and F_p12
     */
    val P = BigInteger("21888242871839275222246405745257275088696311157297823662689037894645226208583")

    /**
     * "r" order of [BN128G2] cyclic subgroup
     */
    val R = BigInteger("21888242871839275222246405745257275088548364400416034343698204186575808495617")

    /**
     * "b" curve parameter for [BN128Fp]
     */
    val B_Fp = Fp.create(BigInteger.valueOf(3))

    /**
     * Twist parameter for the curves
     */
    val TWIST = Fp2.create(BigInteger.valueOf(9), BigInteger.valueOf(1))

    /**
     * "b" curve parameter for [BN128Fp2]
     */
    val B_Fp2 = B_Fp.mul(TWIST.inverse())

    val TWIST_MUL_BY_P_X = Fp2.create(
        BigInteger("21575463638280843010398324269430826099269044274347216827212613867836435027261"),
        BigInteger("10307601595873709700152284273816112264069230130616436755625194854815875713954")
    )

    val TWIST_MUL_BY_P_Y = Fp2.create(
        BigInteger("2821565182194536844548159561693502659359617185244120367078079554186484126554"),
        BigInteger("3505843767911556378687030309984248845540243509899259641013678093033130930403")
    )

    val PAIRING_FINAL_EXPONENT_Z = BigInteger("4965661367192848881")
}