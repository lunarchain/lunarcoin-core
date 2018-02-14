package io.lunarchain.lunarcoin.core

import io.lunarchain.lunarcoin.util.BlockChainUtil
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.junit.Test
import kotlin.test.assertEquals

class BlockChainUtilTest {

    @Test
    fun calculateDifficultyTest() {
        val format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
        val time = DateTime.parse("2017-08-28 11:25:43", format).millis / 1000
        val parentTime = DateTime.parse("2017-08-28 11:24:23", format).millis / 1000
        val difficulty = BlockChainUtil.calculateDifficulty(4212372, time, parentTime, 2117963098883076)
        assertEquals(difficulty, 2111823478825220)
    }

    @Test
    fun calculateDifficulty2Test() {
        val format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
        val time = DateTime.parse("2017-02-19 22:01:57", format).millis / 1000
        val parentTime = DateTime.parse("2017-02-19 22:01:47", format).millis / 1000
        val difficulty = BlockChainUtil.calculateDifficulty(3212372, time, parentTime, 137172791883007)
        assertEquals(difficulty, 137173865624831)
    }
}