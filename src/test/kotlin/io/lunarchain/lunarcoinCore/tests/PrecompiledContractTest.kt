package io.lunarchain.lunarcoinCore.tests

import io.lunarchain.lunarcoin.util.ByteUtil
import io.lunarchain.lunarcoin.util.ByteUtil.EMPTY_BYTE_ARRAY
import io.lunarchain.lunarcoin.util.ByteUtil.bytesToBigInteger
import io.lunarchain.lunarcoin.vm.PrecompiledContracts
import lunar.vm.DataWord
import org.junit.Assert.*
import org.junit.Test
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger

class PrecompiledContractTest {

    @Test
    fun identityTest1() {

        val addr = DataWord("0000000000000000000000000000000000000000000000000000000000000004")
        val contract = PrecompiledContracts.getContractForAddress(addr)
        val data = Hex.decode("112233445566")
        val expected = Hex.decode("112233445566")

        val result = contract?.execute(data)?.second

        assertArrayEquals(expected, result)
    }

    @Test
    fun sha256Test1() {

        val addr = DataWord("0000000000000000000000000000000000000000000000000000000000000002")
        val contract = PrecompiledContracts.getContractForAddress(addr)
        val data: ByteArray? = null
        val expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

        val result = contract?.execute(data)?.second

        assertEquals(expected, Hex.toHexString(result))
    }

    @Test
    fun sha256Test2() {

        val addr = DataWord("0000000000000000000000000000000000000000000000000000000000000002")
        val contract = PrecompiledContracts.getContractForAddress(addr)
        val data = ByteUtil.EMPTY_BYTE_ARRAY
        val expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

        val result = contract?.execute(data)?.second

        assertEquals(expected, Hex.toHexString(result))
    }

    @Test
    fun sha256Test3() {

        val addr = DataWord("0000000000000000000000000000000000000000000000000000000000000002")
        val contract = PrecompiledContracts.getContractForAddress(addr)
        val data = Hex.decode("112233")
        val expected = "49ee2bf93aac3b1fb4117e59095e07abe555c3383b38d608da37680a406096e8"

        val result = contract?.execute(data)?.second

        assertEquals(expected, Hex.toHexString(result))
    }

    @Test
    fun Ripempd160Test1() {

        val addr = DataWord("0000000000000000000000000000000000000000000000000000000000000003")
        val contract = PrecompiledContracts.getContractForAddress(addr)
        val data = Hex.decode("0000000000000000000000000000000000000000000000000000000000000001")
        val expected = "000000000000000000000000ae387fcfeb723c3f5964509af111cf5a67f30661"

        val result = contract?.execute(data)?.second

        assertEquals(expected, Hex.toHexString(result))
    }

    @Test
    fun ecRecoverTest1() {

        val data =
            Hex.decode("18c547e4f7b0f325ad1e56f57e26c745b09a3e503d86e00e5255ff7f715d3d1c000000000000000000000000000000000000000000000000000000000000001c73b1693892219d736caba55bdb67216e485557ea6b6af75f37096c9aa6a5a75feeb940b1d03b21e36b0e47e79769f095fe2ab855bd91e3a38756b7d75a9c4549")
        val addr = DataWord("0000000000000000000000000000000000000000000000000000000000000001")
        val contract = PrecompiledContracts.getContractForAddress(addr)
        val expected = "000000000000000000000000ae387fcfeb723c3f5964509af111cf5a67f30661"

        val result = contract?.execute(data)?.second

        println(Hex.toHexString(result))


    }


    @Test
 fun modExpTest() {

val addr = DataWord("0000000000000000000000000000000000000000000000000000000000000005")


val contract = PrecompiledContracts.getContractForAddress(addr)
assertNotNull(contract)

val data1 = Hex.decode(
    "0000000000000000000000000000000000000000000000000000000000000001" +
    "0000000000000000000000000000000000000000000000000000000000000020" +
    "0000000000000000000000000000000000000000000000000000000000000020" +
    "03" +
    "fffffffffffffffffffffffffffffffffffffffffffffffffffffffefffffc2e" +
    "fffffffffffffffffffffffffffffffffffffffffffffffffffffffefffffc2f"
)

assertEquals(13056.toLong(), contract?.getGasForData(data1))

val res1 = contract?.execute(data1)?.second
assertEquals(32.toLong(), res1?.size?.toLong())
assertEquals(BigInteger.ONE, bytesToBigInteger(res1))

val data2 = Hex.decode(
("0000000000000000000000000000000000000000000000000000000000000000" +
"0000000000000000000000000000000000000000000000000000000000000020" +
"0000000000000000000000000000000000000000000000000000000000000020" +
"fffffffffffffffffffffffffffffffffffffffffffffffffffffffefffffc2e" +
"fffffffffffffffffffffffffffffffffffffffffffffffffffffffefffffc2f"))

assertEquals(13056.toLong(), contract?.getGasForData(data2))

val res2 = contract?.execute(data2)?.second
assertEquals(32.toLong(), res2?.size?.toLong())
assertEquals(BigInteger.ZERO, bytesToBigInteger(res2))

val data3 = Hex.decode(
("0000000000000000000000000000000000000000000000000000000000000000" +
"0000000000000000000000000000000000000000000000000000000000000020" +
"ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff" +
"fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffe" +
"fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffd"))

 // hardly imagine this value could be a real one
        assertEquals(3_674_950_435_109_146_392L, contract?.getGasForData(data3))

val data4 = Hex.decode(
("0000000000000000000000000000000000000000000000000000000000000001" +
"0000000000000000000000000000000000000000000000000000000000000002" +
"0000000000000000000000000000000000000000000000000000000000000020" +
"03" +
"ffff" +
"8000000000000000000000000000000000000000000000000000000000000000" +
"07")) // "07" should be ignored by data parser

assertEquals(768.toLong(), contract?.getGasForData(data4))

val res4 = contract?.execute(data4)?.second
assertEquals(32.toLong(), res4?.size?.toLong())
assertEquals(BigInteger("26689440342447178617115869845918039756797228267049433585260346420242739014315"), bytesToBigInteger(res4))

val data5 = Hex.decode(
("0000000000000000000000000000000000000000000000000000000000000001" +
"0000000000000000000000000000000000000000000000000000000000000002" +
"0000000000000000000000000000000000000000000000000000000000000020" +
"03" +
"ffff" +
"80")) // "80" should be parsed as "8000000000000000000000000000000000000000000000000000000000000000"
 // cause call data is infinitely right-padded with zero bytes

        assertEquals(768.toLong(), contract?.getGasForData(data5))

val res5 = contract?.execute(data5)?.second
assertEquals(32.toLong(), res5?.size?.toLong())
assertEquals(BigInteger("26689440342447178617115869845918039756797228267049433585260346420242739014315"), bytesToBigInteger(res5))

 // check overflow handling in gas calculation
        val data6 = Hex.decode(
("0000000000000000000000000000000000000000000000000000000000000020" +
"0000000000000000000000000000000020000000000000000000000000000000" +
"ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff" +
"fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffe" +
"fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffd" +
"fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffd"))

assertEquals(java.lang.Long.MAX_VALUE, contract?.getGasForData(data6))

 // check rubbish data
        val data7 = Hex.decode(
("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff" +
"ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff" +
"ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff" +
"fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffe" +
"fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffd" +
"fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffd"))

assertEquals(java.lang.Long.MAX_VALUE, contract?.getGasForData(data7))

 // check empty data
        val data8 = ByteArray(0)

assertEquals(0.toLong(), contract?.getGasForData(data8))

val res8 = contract?.execute(data8)?.second
assertArrayEquals(EMPTY_BYTE_ARRAY, res8)

assertEquals(0.toLong(), contract?.getGasForData(null))
assertArrayEquals(EMPTY_BYTE_ARRAY, contract?.execute(null)?.second)
}




}