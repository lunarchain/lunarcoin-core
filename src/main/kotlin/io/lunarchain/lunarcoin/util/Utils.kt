package io.lunarchain.lunarcoin.util

import lunar.vm.DataWord
import java.util.*

object Utils {

    private val DIVISOR = DataWord(64)

    fun repeat(s: String, n: Int): String {
        if (s.length == 1) {
            val bb = ByteArray(n)
            Arrays.fill(bb, s.toByteArray()[0])
            return String(bb)
        } else {
            val ret = StringBuilder()
            for (i in 0 until n) ret.append(s)
            return ret.toString()
        }
    }

    fun align(s: String, fillChar: Char, targetLen: Int, alignRight: Boolean): String {
        if (targetLen <= s.length) return s
        val alignString = repeat("" + fillChar, targetLen - s.length)
        return if (alignRight) alignString.toString() + s else s + alignString

    }

    fun allButOne64th(dw: DataWord): DataWord {
        val ret = dw.clone()
        val d = dw.clone()
        d.div(DIVISOR)
        ret.sub(d)
        return ret
    }
}