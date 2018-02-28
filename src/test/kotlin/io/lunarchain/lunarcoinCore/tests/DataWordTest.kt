package io.lunarchain.lunarcoinCore.tests

import lunar.vm.DataWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger

class DataWordTest {

    @Test
    fun testAddPerformance() {
        //TODO needs to be configured in config files
        val enabled = false

        if (enabled) {
            val one = byteArrayOf(
                0x01,
                0x31,
                0x54,
                0x41,
                0x01,
                0x31,
                0x54,
                0x41,
                0x01,
                0x31,
                0x54,
                0x41,
                0x01,
                0x31,
                0x54,
                0x41,
                0x01,
                0x31,
                0x54,
                0x41,
                0x01,
                0x31,
                0x54,
                0x41,
                0x01,
                0x31,
                0x54,
                0x41,
                0x01,
                0x31,
                0x54,
                0x41
            ) // Random value

            val ITERATIONS = 100

            val now1 = System.currentTimeMillis()
            for (i in 0 until ITERATIONS) {
                val x = DataWord(one)
                x.add(x)
            }
            println("Add1: " + (System.currentTimeMillis() - now1) + "ms")

            val now2 = System.currentTimeMillis()
            for (i in 0 until ITERATIONS) {
                val x = DataWord(one)
                x.add2(x)
            }
            println("Add2: " + (System.currentTimeMillis() - now2) + "ms")
        } else {
            println("ADD performance test is disabled.")
        }
    }

    @Test
    fun testAdd2() {
        val two = ByteArray(32)
        two[31] = 0xff.toByte() // 0x000000000000000000000000000000000000000000000000000000000000ff

        val x = DataWord(two)
        x.add(DataWord(two))
        println(Hex.toHexString(x.getData()))

        val y = DataWord(two)
        y.add2(DataWord(two))
        println(Hex.toHexString(y.getData()))
    }

    @Test
    fun testAdd3() {
        val three = ByteArray(32)
        for (i in three.indices) {
            three[i] = 0xff.toByte()
        }

        val x = DataWord(three)
        x.add(DataWord(three))
        assertEquals(32, x.getData().size)
        println(Hex.toHexString(x.getData()))

        // FAIL
        //      DataWord y = new DataWord(three);
        //      y.add2(new DataWord(three));
        //      System.out.println(Hex.toHexString(y.getData()));
    }

    @Test
    fun testMod() {
        val expected = "000000000000000000000000000000000000000000000000000000000000001a"

        val one = ByteArray(32)
        one[31] = 0x1e // 0x000000000000000000000000000000000000000000000000000000000000001e

        val two = ByteArray(32)
        for (i in two.indices) {
            two[i] = 0xff.toByte()
        }
        two[31] = 0x56 // 0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff56

        val x = DataWord(one)// System.out.println(x.value());
        val y = DataWord(two)// System.out.println(y.value());
        y.mod(x)
        assertEquals(32, y.getData().size)
        assertEquals(expected, Hex.toHexString(y.getData()))
    }

    @Test
    fun testMul() {
        val one = ByteArray(32)
        one[31] = 0x1 // 0x0000000000000000000000000000000000000000000000000000000000000001

        val two = ByteArray(32)
        two[11] = 0x1 // 0x0000000000000000000000010000000000000000000000000000000000000000

        val x = DataWord(one)// System.out.println(x.value());
        val y = DataWord(two)// System.out.println(y.value());
        x.mul(y)
        assertEquals(32, y.getData().size)
        assertEquals("0000000000000000000000010000000000000000000000000000000000000000", Hex.toHexString(y.getData()))
    }

    @Test
    fun testMulOverflow() {

        val one = ByteArray(32)
        one[30] = 0x1 // 0x0000000000000000000000000000000000000000000000000000000000000100

        val two = ByteArray(32)
        two[0] = 0x1 //  0x1000000000000000000000000000000000000000000000000000000000000000

        val x = DataWord(one)// System.out.println(x.value());
        val y = DataWord(two)// System.out.println(y.value());
        x.mul(y)
        assertEquals(32, y.getData().size)
        assertEquals("0100000000000000000000000000000000000000000000000000000000000000", Hex.toHexString(y.getData()))
    }

    @Test
    fun testDiv() {
        val one = ByteArray(32)
        one[30] = 0x01
        one[31] = 0x2c // 0x000000000000000000000000000000000000000000000000000000000000012c

        val two = ByteArray(32)
        two[31] = 0x0f // 0x000000000000000000000000000000000000000000000000000000000000000f

        val x = DataWord(one)
        val y = DataWord(two)
        x.div(y)

        assertEquals(32, x.getData().size)
        assertEquals("0000000000000000000000000000000000000000000000000000000000000014", Hex.toHexString(x.getData()))
    }

    @Test
    fun testDivZero() {
        val one = ByteArray(32)
        one[30] = 0x05 // 0x0000000000000000000000000000000000000000000000000000000000000500

        val two = ByteArray(32)

        val x = DataWord(one)
        val y = DataWord(two)
        x.div(y)

        assertEquals(32, x.getData().size)
        assertTrue(x.isZero())
    }


    @Test
    fun testSDivNegative() {

        // one is -300 as 256-bit signed integer:
        val one = Hex.decode("fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffed4")

        val two = ByteArray(32)
        two[31] = 0x0f

        val x = DataWord(one)
        val y = DataWord(two)
        x.sDiv(y)

        assertEquals(32, x.getData().size)
        assertEquals("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffec", x.toString())
    }

    @Test
    fun testPow() {

        val x = BigInteger.valueOf(Integer.MAX_VALUE.toLong())
        val y = BigInteger.valueOf(1000)

        val result1 = x.modPow(x, y)
        val result2 = pow(x, y)
        println(result1)
        println(result2)
    }

    @Test
    fun testSignExtend1() {

        val x = DataWord(Hex.decode("f2"))
        val k: Byte = 0
        val expected = "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff2"

        x.signExtend(k)
        System.out.println(x.toString())
        assertEquals(expected, x.toString())
    }

    @Test
    fun testSignExtend2() {
        val x = DataWord(Hex.decode("f2"))
        val k: Byte = 1
        val expected = "00000000000000000000000000000000000000000000000000000000000000f2"

        x.signExtend(k)
        System.out.println(x.toString())
        assertEquals(expected, x.toString())
    }

    @Test
    fun testSignExtend3() {

        val k: Byte = 1
        val x = DataWord(Hex.decode("0f00ab"))
        val expected = "00000000000000000000000000000000000000000000000000000000000000ab"

        x.signExtend(k)
        System.out.println(x.toString())
        assertEquals(expected, x.toString())
    }

    @Test
    fun testSignExtend4() {

        val k: Byte = 1
        val x = DataWord(Hex.decode("ffff"))
        val expected = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"

        x.signExtend(k)
        System.out.println(x.toString())
        assertEquals(expected, x.toString())
    }

    @Test
    fun testSignExtend5() {

        val k: Byte = 3
        val x = DataWord(Hex.decode("ffffffff"))
        val expected = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"

        x.signExtend(k)
        System.out.println(x.toString())
        assertEquals(expected, x.toString())
    }

    @Test
    fun testSignExtend6() {

        val k: Byte = 3
        val x = DataWord(Hex.decode("ab02345678"))
        val expected = "0000000000000000000000000000000000000000000000000000000002345678"

        x.signExtend(k)
        System.out.println(x.toString())
        assertEquals(expected, x.toString())
    }

    @Test
    fun testSignExtend7() {

        val k: Byte = 3
        val x = DataWord(Hex.decode("ab82345678"))
        val expected = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffff82345678"

        x.signExtend(k)
        System.out.println(x.toString())
        assertEquals(expected, x.toString())
    }

    @Test
    fun testSignExtend8() {

        val k: Byte = 30
        val x = DataWord(Hex.decode("ff34567882345678823456788234567882345678823456788234567882345678"))
        val expected = "0034567882345678823456788234567882345678823456788234567882345678"

        x.signExtend(k)
        System.out.println(x.toString())
        assertEquals(expected, x.toString())
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun testSignExtendException1() {

        val k: Byte = -1
        val x = DataWord()

        x.signExtend(k) // should throw an exception
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun testSignExtendException2() {

        val k: Byte = 32
        val x = DataWord()

        x.signExtend(k) // should throw an exception
    }

    @Test
    fun testAddModOverflow() {
        testAddMod(
            "9999999999999999999999999999999999999999999999999999999999999999",
            "8888888888888888888888888888888888888888888888888888888888888888",
            "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
        )
        testAddMod(
            "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
            "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
            "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
        )
    }

    internal fun testAddMod(v1: String, v2: String, v3: String) {
        val dv1 = DataWord(Hex.decode(v1))
        val dv2 = DataWord(Hex.decode(v2))
        val dv3 = DataWord(Hex.decode(v3))
        val bv1 = BigInteger(v1, 16)
        val bv2 = BigInteger(v2, 16)
        val bv3 = BigInteger(v3, 16)

        dv1.addmod(dv2, dv3)
        val br = bv1.add(bv2).mod(bv3)
        assertEquals(dv1.value(), br)
    }

    @Test
    fun testMulMod1() {
        val wr = DataWord(Hex.decode("9999999999999999999999999999999999999999999999999999999999999999"))
        val w1 = DataWord(Hex.decode("01"))
        val w2 = DataWord(Hex.decode("9999999999999999999999999999999999999999999999999999999999999998"))

        wr.mulmod(w1, w2)

        assertEquals(32, wr.getData().size)
        assertEquals("0000000000000000000000000000000000000000000000000000000000000001", Hex.toHexString(wr.getData()))
    }

    @Test
    fun testMulMod2() {
        val wr = DataWord(Hex.decode("9999999999999999999999999999999999999999999999999999999999999999"))
        val w1 = DataWord(Hex.decode("01"))
        val w2 = DataWord(Hex.decode("9999999999999999999999999999999999999999999999999999999999999999"))

        wr.mulmod(w1, w2)

        assertEquals(32, wr.getData().size)
        assertTrue(wr.isZero())
    }

    @Test
    fun testMulModZero() {
        val wr = DataWord(Hex.decode("00"))
        val w1 = DataWord(Hex.decode("9999999999999999999999999999999999999999999999999999999999999999"))
        val w2 = DataWord(Hex.decode("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"))

        wr.mulmod(w1, w2)

        assertEquals(32, wr.getData().size)
        assertTrue(wr.isZero())
    }

    @Test
    fun testMulModZeroWord1() {
        val wr = DataWord(Hex.decode("9999999999999999999999999999999999999999999999999999999999999999"))
        val w1 = DataWord(Hex.decode("00"))
        val w2 = DataWord(Hex.decode("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"))

        wr.mulmod(w1, w2)

        assertEquals(32, wr.getData().size)
        assertTrue(wr.isZero())
    }

    @Test
    fun testMulModZeroWord2() {
        val wr = DataWord(Hex.decode("9999999999999999999999999999999999999999999999999999999999999999"))
        val w1 = DataWord(Hex.decode("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"))
        val w2 = DataWord(Hex.decode("00"))

        wr.mulmod(w1, w2)

        assertEquals(32, wr.getData().size)
        assertTrue(wr.isZero())
    }

    @Test
    fun testMulModOverflow() {
        val wr = DataWord(Hex.decode("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"))
        val w1 = DataWord(Hex.decode("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"))
        val w2 = DataWord(Hex.decode("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"))

        wr.mulmod(w1, w2)

        assertEquals(32, wr.getData().size)
        assertTrue(wr.isZero())
    }

    fun pow(x: BigInteger, y: BigInteger): BigInteger {
        if (y.compareTo(BigInteger.ZERO) < 0)
            throw IllegalArgumentException()
        var z = x // z will successively become x^2, x^4, x^8, x^16,
        // x^32...
        var result = BigInteger.ONE
        val bytes = y.toByteArray()
        for (i in bytes.indices.reversed()) {
            var bits = bytes[i]
            for (j in 0..7) {
                if (bits.toInt() and 1 != 0)
                    result = result.multiply(z)
                // short cut out if there are no more bits to handle:
                if ((bits.toInt() shr 1) == 0 && i == 0)
                    return result
                z = z.multiply(z)
            }
        }
        return result
    }

}