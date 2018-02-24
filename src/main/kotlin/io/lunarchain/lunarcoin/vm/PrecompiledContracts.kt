package io.lunarchain.lunarcoin.vm

import io.lunarchain.lunarcoin.util.BIUtil
import io.lunarchain.lunarcoin.util.BIUtil.addSafely
import io.lunarchain.lunarcoin.util.BIUtil.isLessThan
import io.lunarchain.lunarcoin.util.BIUtil.isZero
import io.lunarchain.lunarcoin.util.ByteUtil.EMPTY_BYTE_ARRAY
import io.lunarchain.lunarcoin.util.ByteUtil.bytesToBigInteger
import io.lunarchain.lunarcoin.util.ByteUtil.numberOfLeadingZeros
import io.lunarchain.lunarcoin.util.ByteUtil.parseBytes
import io.lunarchain.lunarcoin.util.ByteUtil.parseWord
import io.lunarchain.lunarcoin.util.ByteUtil.stripLeadingZeroes
import io.lunarchain.lunarcoin.util.CryptoUtil
import io.lunarchain.lunarcoin.util.HashUtil
import io.lunarchain.lunarcoin.util.zksnark.BN128Fp
import io.lunarchain.lunarcoin.util.zksnark.BN128G1
import io.lunarchain.lunarcoin.util.zksnark.BN128G2
import io.lunarchain.lunarcoin.util.zksnark.PairingCheck
import lunar.vm.DataWord
import java.math.BigInteger

object PrecompiledContracts {
    private val ecRecover = ECRecover()
    private val sha256 = Sha256()
    private val ripempd160 = Ripempd160()
    private val identity = Identity()
    private val modExp = ModExp()
    private val altBN128Add = BN128Addition()
    private val altBN128Mul = BN128Multiplication()
    private val altBN128Pairing = BN128Pairing()

    private val ecRecoverAddr = DataWord("0000000000000000000000000000000000000000000000000000000000000001")
    private val sha256Addr = DataWord("0000000000000000000000000000000000000000000000000000000000000002")
    private val ripempd160Addr = DataWord("0000000000000000000000000000000000000000000000000000000000000003")
    private val identityAddr = DataWord("0000000000000000000000000000000000000000000000000000000000000004")
    private val modExpAddr = DataWord("0000000000000000000000000000000000000000000000000000000000000005")
    private val altBN128AddAddr = DataWord("0000000000000000000000000000000000000000000000000000000000000006")
    private val altBN128MulAddr = DataWord("0000000000000000000000000000000000000000000000000000000000000007")
    private val altBN128PairingAddr = DataWord("0000000000000000000000000000000000000000000000000000000000000008")

    fun getContractForAddress(address: DataWord?): PrecompiledContract? {

        if (address == null) return identity
        if (address.equals(ecRecoverAddr)) return ecRecover
        if (address.equals(sha256Addr)) return sha256
        if (address.equals(ripempd160Addr)) return ripempd160
        if (address.equals(identityAddr)) return identity

        // Byzantium precompiles
        if (address.equals(modExpAddr)) return modExp
        if (address.equals(altBN128AddAddr)) return altBN128Add
        if (address.equals(altBN128MulAddr)) return altBN128Mul
        return if (address.equals(altBN128PairingAddr)) altBN128Pairing else null

    }

    private fun encodeRes(w1: ByteArray?, w2: ByteArray?): ByteArray {
        var w1 = w1
        var w2 = w2

        val res = ByteArray(64)

        w1 = stripLeadingZeroes(w1)
        w2 = stripLeadingZeroes(w2)

        System.arraycopy(w1, 0, res, 32 - w1!!.size, w1.size)
        System.arraycopy(w2, 0, res, 64 - w2!!.size, w2.size)

        return res
    }

    abstract class PrecompiledContract {
        abstract fun getGasForData(data: ByteArray?): Long

        abstract fun execute(data: ByteArray?): Pair<Boolean, ByteArray?>
    }


    class Identity : PrecompiledContract() {

        override fun getGasForData(data: ByteArray?): Long {
            // gas charge for the execution:
            // minimum 1 and additional 1 for each 32 bytes word (round  up)
            return if (data == null) 15 else (15 + (data.size + 31) / 32 * 3).toLong()
        }

        override fun execute(data: ByteArray?): Pair<Boolean, ByteArray?> {
            return Pair(true, data)
        }
    }

    class Sha256 : PrecompiledContract() {


        override fun getGasForData(data: ByteArray?): Long {

            // gas charge for the execution:
            // minimum 50 and additional 50 for each 32 bytes word (round  up)
            return if (data == null) 60 else (60 + (data.size + 31) / 32 * 12).toLong()
        }

        override fun execute(data: ByteArray?): Pair<Boolean, ByteArray> {

            return if (data == null) Pair(true, HashUtil.sha256(EMPTY_BYTE_ARRAY)) else Pair(true, HashUtil.sha256(data))
        }
    }

    class Ripempd160 : PrecompiledContract() {


        override fun getGasForData(data: ByteArray?): Long {

            // TODO #POC9 Replace magic numbers with constants
            // gas charge for the execution:
            // minimum 50 and additional 50 for each 32 bytes word (round  up)
            return if (data == null) 600 else (600 + (data.size + 31) / 32 * 120).toLong()
        }

        override fun execute(data: ByteArray?): Pair<Boolean, ByteArray?> {

            var result: ByteArray? = null
            if (data == null)
                result = HashUtil.ripemd160(EMPTY_BYTE_ARRAY)
            else
                result = HashUtil.ripemd160(data)

            return Pair(true, DataWord(result).getData())
        }
    }


    class ECRecover : PrecompiledContract() {

        override fun getGasForData(data: ByteArray?): Long {
            return 3000
        }

        override fun execute(data: ByteArray?): Pair<Boolean, ByteArray?> {

            val h = ByteArray(32)
            val v = ByteArray(32)
            val r = ByteArray(32)
            val s = ByteArray(32)

            var out: DataWord? = null

            try {
                System.arraycopy(data, 0, h, 0, 32)
                System.arraycopy(data, 32, v, 0, 32)
                System.arraycopy(data, 64, r, 0, 32)

                val sLength = if (data!!.size < 128) data.size - 96 else 32
                System.arraycopy(data, 96, s, 0, sLength)

                val signature = CryptoUtil.Companion.ECDSASignature().fromComponents(r, s, v[31])
                if (validateV(v) && signature.validateComponents()) {
                    out = DataWord(CryptoUtil.Companion.signatureToAddress(h, signature))
                }
            } catch (any: Throwable) {
            }

            return if (out == null) {
                Pair(true, EMPTY_BYTE_ARRAY)
            } else {
                Pair(true, out.getData())
            }
        }

        private fun validateV(v: ByteArray): Boolean {
            for (i in 0 until v.size - 1) {
                if (v[i].toInt() != 0) return false
            }
            return true
        }
    }


    /**
     * Computes modular exponentiation on big numbers
     *
     * format of data[] array:
     * [length_of_BASE] [length_of_EXPONENT] [length_of_MODULUS] [BASE] [EXPONENT] [MODULUS]
     * where every length is a 32-byte left-padded integer representing the number of bytes.
     * Call data is assumed to be infinitely right-padded with zero bytes.
     *
     * Returns an output as a byte array with the same length as the modulus
     */
    class ModExp : PrecompiledContract() {

        override fun getGasForData(data: ByteArray?): Long {
            var data = data

            if (data == null) data = EMPTY_BYTE_ARRAY

            val baseLen = parseLen(data, 0)
            val expLen = parseLen(data, 1)
            val modLen = parseLen(data, 2)

            val expHighBytes = parseBytes(data, addSafely(ARGS_OFFSET, baseLen), Math.min(expLen, 32))

            val multComplexity = getMultComplexity(Math.max(baseLen, modLen).toLong())
            val adjExpLen = getAdjustedExponentLength(expHighBytes, expLen.toLong())

            // use big numbers to stay safe in case of overflow
            val gas = BigInteger.valueOf(multComplexity)
                .multiply(BigInteger.valueOf(Math.max(adjExpLen, 1)))
                .divide(GQUAD_DIVISOR)

            return if (isLessThan(
                    gas,
                    BigInteger.valueOf(java.lang.Long.MAX_VALUE)
                )
            ) gas.toLong() else java.lang.Long.MAX_VALUE
        }

        override fun execute(data: ByteArray?): Pair<Boolean, ByteArray?> {

            if (data == null)
                return Pair(true, EMPTY_BYTE_ARRAY)

            val baseLen = parseLen(data, 0)
            val expLen = parseLen(data, 1)
            val modLen = parseLen(data, 2)

            val base = parseArg(data, ARGS_OFFSET, baseLen)
            val exp = parseArg(data, addSafely(ARGS_OFFSET, baseLen), expLen)
            val mod = parseArg(data, addSafely(addSafely(ARGS_OFFSET, baseLen), expLen), modLen)

            // check if modulus is zero
            if (isZero(mod))
                return Pair(true, EMPTY_BYTE_ARRAY)

            val res = stripLeadingZeroes(base.modPow(exp, mod).toByteArray())

            // adjust result to the same length as the modulus has
            if (res!!.size < modLen) {

                val adjRes = ByteArray(modLen)
                System.arraycopy(res, 0, adjRes, modLen - res.size, res.size)

                return Pair(true, adjRes)

            } else {
                return Pair(true, res)
            }
        }

        private fun getMultComplexity(x: Long): Long {

            val x2 = x * x

            if (x <= 64) return x2
            return if (x <= 1024) x2 / 4 + 96 * x - 3072 else x2 / 16 + 480 * x - 199680

        }

        private fun getAdjustedExponentLength(expHighBytes: ByteArray, expLen: Long): Long {

            val leadingZeros = numberOfLeadingZeros(expHighBytes)
            var highestBit = 8 * expHighBytes.size - leadingZeros

            // set index basement to zero
            if (highestBit > 0) highestBit--

            return if (expLen <= 32) {
                highestBit.toLong()
            } else {
                8 * (expLen - 32) + highestBit
            }
        }

        private fun parseLen(data: ByteArray, idx: Int): Int {
            val bytes = parseBytes(data, 32 * idx, 32)
            return DataWord(bytes).intValueSafe()
        }

        private fun parseArg(data: ByteArray, offset: Int, len: Int): BigInteger {
            val bytes = parseBytes(data, offset, len)
            return bytesToBigInteger(bytes)
        }

        companion object {

            private val GQUAD_DIVISOR = BigInteger.valueOf(20)

            private val ARGS_OFFSET = 32 * 3 // addresses length part
        }
    }


    /**
     * Computes point addition on Barreto–Naehrig curve.
     * See [BN128Fp] for details<br></br>
     * <br></br>
     *
     * input data[]:<br></br>
     * two points encoded as (x, y), where x and y are 32-byte left-padded integers,<br></br>
     * if input is shorter than expected, it's assumed to be right-padded with zero bytes<br></br>
     * <br></br>
     *
     * output:<br></br>
     * resulting point (x', y'), where x and y encoded as 32-byte left-padded integers<br></br>
     *
     */
    class BN128Addition : PrecompiledContract() {

        override fun getGasForData(data: ByteArray?): Long {
            return 500
        }

        override fun execute(data: ByteArray?): Pair<Boolean, ByteArray?> {
            var data = data

            if (data == null)
                data = EMPTY_BYTE_ARRAY

            val x1 = parseWord(data, 0)
            val y1 = parseWord(data, 1)

            val x2 = parseWord(data, 2)
            val y2 = parseWord(data, 3)

            val p1 = BN128Fp.create(x1, y1) ?: return Pair(false, EMPTY_BYTE_ARRAY)

            val p2 = BN128Fp.create(x2, y2) ?: return Pair(false, EMPTY_BYTE_ARRAY)

            val res = p1.add(p2).toEthNotation()

            return Pair(true, encodeRes(res.x()!!.bytes(), res.y()!!.bytes()))
        }
    }

    /**
     * Computes multiplication of scalar value on a point belonging to Barreto–Naehrig curve.
     * See [BN128Fp] for details<br></br>
     * <br></br>
     *
     * input data[]:<br></br>
     * point encoded as (x, y) is followed by scalar s, where x, y and s are 32-byte left-padded integers,<br></br>
     * if input is shorter than expected, it's assumed to be right-padded with zero bytes<br></br>
     * <br></br>
     *
     * output:<br></br>
     * resulting point (x', y'), where x and y encoded as 32-byte left-padded integers<br></br>
     *
     */
    class BN128Multiplication : PrecompiledContract() {

        override fun getGasForData(data: ByteArray?): Long {
            return 40000
        }

        override fun execute(data: ByteArray?): Pair<Boolean, ByteArray?> {
            var data = data

            if (data == null)
                data = EMPTY_BYTE_ARRAY

            val x = parseWord(data, 0)
            val y = parseWord(data, 1)

            val s = parseWord(data, 2)

            val p = BN128Fp.create(x, y) ?: return Pair(false, EMPTY_BYTE_ARRAY)

            val res = p.mul(BIUtil.toBI(s)).toEthNotation()

            return Pair(true, encodeRes(res.x()!!.bytes(), res.y()!!.bytes()))
        }
    }

    /**
     * Computes pairing check. <br></br>
     * See [PairingCheck] for details.<br></br>
     * <br></br>
     *
     * Input data[]: <br></br>
     * an array of points (a1, b1, ... , ak, bk), <br></br>
     * where "ai" is a point of [BN128Fp] curve and encoded as two 32-byte left-padded integers (x; y) <br></br>
     * "bi" is a point of [BN128G2] curve and encoded as four 32-byte left-padded integers `(ai + b; ci + d)`,
     * each coordinate of the point is a big-endian [Fp2] number, so `b` precedes `a` in the encoding:
     * `(b, a; d, c)` <br></br>
     * thus each pair (ai, bi) has 192 bytes length, if 192 is not a multiple of `data.length` then execution fails <br></br>
     * the number of pairs is derived from input length by dividing it by 192 (the length of a pair) <br></br>
     * <br></br>
     *
     * output: <br></br>
     * pairing product which is either 0 or 1, encoded as 32-byte left-padded integer <br></br>
     *
     */
    class BN128Pairing : PrecompiledContract() {

        override fun getGasForData(data: ByteArray?): Long {

            return if (data == null) 100000 else (80000 * (data.size / PAIR_SIZE) + 100000).toLong()

        }

        override fun execute(data: ByteArray?): Pair<Boolean, ByteArray?> {
            var data = data

            if (data == null)
                data = EMPTY_BYTE_ARRAY

            // fail if input len is not a multiple of PAIR_SIZE
            if (data.size % PAIR_SIZE > 0)
                return Pair(false, EMPTY_BYTE_ARRAY)

            val check = PairingCheck.create()

            // iterating over all pairs
            var offset = 0
            while (offset < data.size) {

                val pair = decodePair(data, offset) ?: return Pair(false, EMPTY_BYTE_ARRAY)

                // fail if decoding has failed

                check.addPair(pair.first, pair.second)
                offset += PAIR_SIZE
            }

            check.run()
            val result = check.result()

            return Pair(true, DataWord(result).getData())
        }

        private fun decodePair(`in`: ByteArray, offset: Int): Pair<BN128G1, BN128G2>? {

            val x = parseWord(`in`, offset, 0)
            val y = parseWord(`in`, offset, 1)

            val p1 = BN128G1.create(x, y) ?: return null

            // fail if point is invalid

            // (b, a)
            val b = parseWord(`in`, offset, 2)
            val a = parseWord(`in`, offset, 3)

            // (d, c)
            val d = parseWord(`in`, offset, 4)
            val c = parseWord(`in`, offset, 5)

            val p2 = BN128G2.create(a, b, c, d) ?: return null

            // fail if point is invalid

            return Pair(p1, p2)
        }

        companion object {

            private val PAIR_SIZE = 192
        }
    }


}