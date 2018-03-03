package io.lunarchain.lunarcoinCore.tests

import io.lunarchain.lunarcoin.util.ByteUtil
import lunar.vm.program.Program
import org.junit.Before
import java.nio.ByteBuffer

class ProgramMemory {


    internal var program: Program? = null
    internal var memory: ByteBuffer? = null

    @Before
    fun createProgram() {
        program = Program(ByteUtil.EMPTY_BYTE_ARRAY, pi)
    }
}