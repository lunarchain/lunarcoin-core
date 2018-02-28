package io.lunarchain.lunarcoinCore.tests

import org.junit.Assert
import org.junit.Test

class BytecodeCompilerTest {
    @Test
    fun compileSimpleOpcode() {
        val compiler = BytecodeCompiler()

        val result = compiler.compile("ADD")

        Assert.assertNotNull(result)
        Assert.assertEquals(1, result.size.toLong())
        Assert.assertEquals(1, result[0].toLong())
    }

    @Test
    fun compileSimpleOpcodeWithSpaces() {
        val compiler = BytecodeCompiler()

        val result = compiler.compile(" ADD ")

        Assert.assertNotNull(result)
        Assert.assertEquals(1, result.size.toLong())
        Assert.assertEquals(1, result[0].toLong())
    }

    @Test
    fun compileTwoOpcodes() {
        val compiler = BytecodeCompiler()

        val result = compiler.compile("ADD SUB")

        Assert.assertNotNull(result)
        Assert.assertEquals(2, result.size.toLong())
        Assert.assertEquals(1, result[0].toLong())
        Assert.assertEquals(3, result[1].toLong())
    }

    @Test
    fun compileFourOpcodes() {
        val compiler = BytecodeCompiler()

        val result = compiler.compile("ADD MUL SUB DIV")

        Assert.assertNotNull(result)
        Assert.assertEquals(4, result.size.toLong())
        Assert.assertEquals(1, result[0].toLong())
        Assert.assertEquals(2, result[1].toLong())
        Assert.assertEquals(3, result[2].toLong())
        Assert.assertEquals(4, result[3].toLong())
    }

    @Test
    fun compileHexadecimalValueOneByte() {
        val compiler = BytecodeCompiler()

        val result = compiler.compile("0x01")

        Assert.assertNotNull(result)
        Assert.assertEquals(1, result.size.toLong())
        Assert.assertEquals(1, result[0].toLong())
    }

    @Test
    fun compileHexadecimalValueTwoByte() {
        val compiler = BytecodeCompiler()

        val result = compiler.compile("0x0102")

        Assert.assertNotNull(result)
        Assert.assertEquals(2, result.size.toLong())
        Assert.assertEquals(1, result[0].toLong())
        Assert.assertEquals(2, result[1].toLong())
    }

    @Test
    fun compileSimpleOpcodeInLowerCase() {
        val compiler = BytecodeCompiler()

        val result = compiler.compile("add")

        Assert.assertNotNull(result)
        Assert.assertEquals(1, result.size.toLong())
        Assert.assertEquals(1, result[0].toLong())
    }

    @Test
    fun compileSimpleOpcodeInMixedCase() {
        val compiler = BytecodeCompiler()

        val result = compiler.compile("Add")

        Assert.assertNotNull(result)
        Assert.assertEquals(1, result.size.toLong())
        Assert.assertEquals(1, result[0].toLong())
    }
}