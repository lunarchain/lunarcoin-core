package io.lunarchain.lunarcoin.core

import io.lunarchain.lunarcoin.trie.*
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class PatriciaTrieTest {

    @Test
    fun testBinToNibbles() {
        val res0 = binToNibbbles("".toByteArray())
        assertArrayEquals(res0, arrayOf<Int>())

        val res1 = binToNibbbles("h".toByteArray())
        assertArrayEquals(res1, arrayOf(6, 8))

        val res2 = binToNibbbles("he".toByteArray())
        assertArrayEquals(res2, arrayOf(6, 8, 6, 5))

        val res3 = binToNibbbles("hello".toByteArray())
        assertArrayEquals(res3, arrayOf(6, 8, 6, 5, 6, 12, 6, 12, 6, 15))
    }

    @Test
    fun testNibblesToBin() {
        val res0 = nibblesToBin(arrayOf())
        assertArrayEquals(res0, "".toByteArray())

        val res1 = nibblesToBin(arrayOf(6, 8))
        assertArrayEquals(res1, "h".toByteArray())

        val res2 = nibblesToBin(arrayOf(6, 8, 6, 5))
        assertArrayEquals(res2, "he".toByteArray())

        val res3 = nibblesToBin(arrayOf(6, 8, 6, 5, 6, 12, 6, 12, 6, 15))
        assertArrayEquals(res3, "hello".toByteArray())
    }

    @Test
    fun testPackNibbles() {
        val key = arrayOf(0, 1, 0, 1, 0, 2)
        val packed = packNibbles(withTerminator(key))

        val expected = arrayOf(0x20.toByte(), 0x01.toByte(), 0x01.toByte(), 0x02.toByte()).toByteArray()
        assertArrayEquals(packed, expected)
    }

    @Test
    fun testUnpackToNibbles() {
        val bin = arrayOf(0x20.toByte(), 0x01.toByte(), 0x01.toByte(), 0x02.toByte()).toByteArray()
        val nibbles = unpackToNibbles(bin)

        assertArrayEquals(nibbles, arrayOf(0, 1, 0, 1, 0, 2, NIBBLE_TERMINATOR))
    }
}
