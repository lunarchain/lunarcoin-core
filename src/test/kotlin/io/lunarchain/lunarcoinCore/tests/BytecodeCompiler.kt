package io.lunarchain.lunarcoinCore.tests

import lunar.vm.OpCode
import org.spongycastle.util.encoders.Hex
import java.util.ArrayList

class BytecodeCompiler {
    fun compile(code: String): ByteArray {
        return compile(code.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
    }

    private fun compile(tokens: Array<String>): ByteArray {
        val bytecodes = ArrayList<Byte>()
        val ntokens = tokens.size

        for (i in 0 until ntokens) {
            val token = tokens[i].trim { it <= ' ' }.toUpperCase()

            if (token.isEmpty())
                continue

            if (isHexadecimal(token))
                compileHexadecimal(token, bytecodes)
            else
                bytecodes.add(OpCode.byteVal(token))
        }

        val nbytes = bytecodes.size
        val bytes = ByteArray(nbytes)

        for (k in 0 until nbytes)
            bytes[k] = bytecodes[k]

        return bytes
    }

    private fun isHexadecimal(token: String): Boolean {
        return token.startsWith("0X")
    }

    private fun compileHexadecimal(token: String, bytecodes: MutableList<Byte>) {
        val bytes = Hex.decode(token.substring(2))

        for (k in bytes.indices)
            bytecodes.add(bytes[k])
    }
}