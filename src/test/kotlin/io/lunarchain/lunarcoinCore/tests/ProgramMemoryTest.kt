package io.lunarchain.lunarcoinCore.tests

import io.lunarchain.lunarcoin.config.BlockChainConfig
import io.lunarchain.lunarcoin.util.ByteUtil
import io.lunarchain.lunarcoin.util.CryptoUtil
import io.lunarchain.lunarcoin.vm.program.invoke.ProgramInvokeImpl
import io.lunarchain.lunarcoin.storage.ServerRepository
import junit.framework.Assert.assertEquals
import lunar.vm.DataWord
import lunar.vm.program.Program
import lunar.vm.program.invoke.ProgramInvoke
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.spongycastle.util.encoders.Hex
import java.nio.ByteBuffer

class ProgramMemoryTest {

    val originPrivKey = CryptoUtil.generateKeyPair().private.encoded
    val originPubKey = CryptoUtil.generatePublicKey(CryptoUtil.deserializePrivateKey(originPrivKey))
    val originAddress = CryptoUtil.generateAddress(originPubKey!!)

    val callerPrivKey = CryptoUtil.generateKeyPair().private.encoded
    val callerPubKey = CryptoUtil.generatePublicKey(CryptoUtil.deserializePrivateKey(callerPrivKey))
    val callerAddress = CryptoUtil.generateAddress(callerPubKey!!)

    val balance = Hex.decode("0DE0B6B3A7640000")
    val minGasPrice = Hex.decode("09184e72a000")
    val gasLimit: Long = 1000000
    val msgData = ByteArray(32)
    val prevHash = Hex.decode("961CB117ABA86D1E596854015A1483323F18883C2D745B0BC03E87F146D2BB1C")
    val coinBase = Hex.decode("E559DE5527492BCB42EC68D07DF0742A98EC3F1E")
    val timestamp: Long = 1401421348
    val number: Long = 33
    val difficulty = Hex.decode("3ED290")
    val blockStore = ServerRepository(BlockChainConfig())



    var programInvoke: ProgramInvoke = ProgramInvokeImpl(
        DataWord("cd2a3d9f938e13cd947ec05abc7fe734df8dd826"),
        DataWord(originAddress),
        DataWord(callerAddress),
        DataWord(balance),
        DataWord(minGasPrice),
        DataWord(gasLimit),
        DataWord(balance),
        msgData,
        DataWord(prevHash),
        DataWord(coinBase),
        DataWord(timestamp),
        DataWord(number),
        DataWord(difficulty),
        DataWord(gasLimit),
        blockStore,
        0,
        false,
        false

        )
    var program: Program? = null
    var memory: ByteBuffer? = null


    @Before
    fun createProgram() {
        program = Program(ByteUtil.EMPTY_BYTE_ARRAY, programInvoke)
    }

    @Test
    fun testGetMemSize() {
        val memory = ByteArray(64)
        program!!.initMem(memory)
        assertEquals(64, program!!.getMemSize())
    }

    @Test
    fun testMemoryChunk1() {
        program!!.initMem(ByteArray(64))
        val offset = 128
        val size = 32
        program!!.memoryChunk(offset, size)
        assertEquals(160, program!!.getMemSize())
    }

    @Test // size 0 doesn't increase memory
    fun testMemoryChunk2() {
        program!!.initMem(ByteArray(64))
        val offset = 96
        val size = 0
        program!!.memoryChunk(offset, size)
        assertEquals(64, program!!.getMemSize())
    }


    @Test
    fun testAllocateMemory1() {

        program!!.initMem(ByteArray(64))
        val offset = 32
        val size = 32
        program!!.allocateMemory(offset, size)
        assertEquals(64, program!!.getMemSize())
    }

    @Test
    fun testAllocateMemory2() {

        // memory.limit() > offset, == size
        // memory.limit() < offset + size
        program!!.initMem(ByteArray(64))
        val offset = 32
        val size = 64
        program!!.allocateMemory(offset, size)
        assertEquals(96, program!!.getMemSize())
    }

    @Test
    fun testAllocateMemory3() {

        // memory.limit() > offset, > size
        program!!.initMem(ByteArray(64))
        val offset = 0
        val size = 32
        program!!.allocateMemory(offset, size)
        assertEquals(64, program!!.getMemSize())
    }

    @Test
    fun testAllocateMemory4() {

        program!!.initMem(ByteArray(64))
        val offset = 0
        val size = 64
        program!!.allocateMemory(offset, size)
        assertEquals(64, program!!.getMemSize())
    }

    @Test
    fun testAllocateMemory5() {

        program!!.initMem(ByteArray(64))
        val offset = 0
        val size = 0
        program!!.allocateMemory(offset, size)
        assertEquals(64, program!!.getMemSize())
    }

    @Test
    fun testAllocateMemory6() {

        // memory.limit() == offset, > size
        program!!.initMem(ByteArray(64))
        val offset = 64
        val size = 32
        program!!.allocateMemory(offset, size)
        assertEquals(96, program!!.getMemSize())
    }

    @Test
    fun testAllocateMemory7() {

        // memory.limit() == offset - size
        program!!.initMem(ByteArray(64))
        val offset = 96
        val size = 32
        program!!.allocateMemory(offset, size)
        assertEquals(128, program!!.getMemSize())
    }

    @Test
    fun testAllocateMemory8() {

        program!!.initMem(ByteArray(64))
        val offset = 0
        val size = 96
        program!!.allocateMemory(offset, size)
        assertEquals(96, program!!.getMemSize())
    }

    @Test
    fun testAllocateMemory9() {

        // memory.limit() < offset, > size
        // memory.limit() < offset - size
        program!!.initMem(ByteArray(64))
        val offset = 96
        val size = 0
        program!!.allocateMemory(offset, size)
        assertEquals(64, program!!.getMemSize())
    }

    /** */


    @Test
    fun testAllocateMemory10() {

        // memory = null, offset > size
        val offset = 32
        val size = 0
        program!!.allocateMemory(offset, size)
        assertEquals(0, program!!.getMemSize())
    }

    @Test
    fun testAllocateMemory11() {

        // memory = null, offset < size
        val offset = 0
        val size = 32
        program!!.allocateMemory(offset, size)
        assertEquals(32, program!!.getMemSize())
    }

    @Test
    fun testAllocateMemory12() {

        // memory.limit() < offset, < size
        program!!.initMem(ByteArray(64))
        val offset = 64
        val size = 96
        program!!.allocateMemory(offset, size)
        assertEquals(160, program!!.getMemSize())
    }

    @Test
    fun testAllocateMemory13() {

        // memory.limit() > offset, < size
        program!!.initMem(ByteArray(64))
        val offset = 32
        val size = 128
        program!!.allocateMemory(offset, size)
        assertEquals(160, program!!.getMemSize())
    }

    @Test
    fun testAllocateMemory14() {

        // memory.limit() < offset, == size
        program!!.initMem(ByteArray(64))
        val offset = 96
        val size = 64
        program!!.allocateMemory(offset, size)
        assertEquals(160, program!!.getMemSize())
    }

    @Test
    fun testAllocateMemory15() {

        // memory.limit() == offset, < size
        program!!.initMem(ByteArray(64))
        val offset = 64
        val size = 96
        program!!.allocateMemory(offset, size)
        assertEquals(160, program!!.getMemSize())
    }

    @Test
    fun testAllocateMemory16() {

        // memory.limit() == offset, == size
        // memory.limit() > offset - size
        program!!.initMem(ByteArray(64))
        val offset = 64
        val size = 64
        program!!.allocateMemory(offset, size)
        assertEquals(128, program!!.getMemSize())
    }

    @Test
    fun testAllocateMemory17() {

        // memory.limit() > offset + size
        program!!.initMem(ByteArray(96))
        val offset = 32
        val size = 32
        program!!.allocateMemory(offset, size)
        assertEquals(96, program!!.getMemSize())
    }

    @Test
    fun testAllocateMemoryUnrounded1() {

        // memory unrounded
        program!!.initMem(ByteArray(64))
        val offset = 64
        val size = 32
        program!!.allocateMemory(offset, size)
        assertEquals(96, program!!.getMemSize())
    }

    @Test
    fun testAllocateMemoryUnrounded2() {

        // offset unrounded
        program!!.initMem(ByteArray(64))
        val offset = 16
        val size = 32
        program!!.allocateMemory(offset, size)
        assertEquals(64, program!!.getMemSize())
    }

    @Test
    fun testAllocateMemoryUnrounded3() {

        // size unrounded
        program!!.initMem(ByteArray(64))
        val offset = 64
        val size = 16
        program!!.allocateMemory(offset, size)
        assertEquals(96, program!!.getMemSize())
    }

    @Test
    fun testAllocateMemoryUnrounded4() {

        // memory + offset unrounded
        program!!.initMem(ByteArray(64))
        val offset = 16
        val size = 32
        program!!.allocateMemory(offset, size)
        assertEquals(64, program!!.getMemSize())
    }

    @Test
    fun testAllocateMemoryUnrounded5() {

        // memory + size unrounded
        program!!.initMem(ByteArray(64))
        val offset = 32
        val size = 16
        program!!.allocateMemory(offset, size)
        assertEquals(64, program!!.getMemSize())
    }

    @Test
    fun testAllocateMemoryUnrounded6() {

        // offset + size unrounded
        program!!.initMem(ByteArray(32))
        val offset = 16
        val size = 16
        program!!.allocateMemory(offset, size)
        assertEquals(32, program!!.getMemSize())
    }

    @Test
    fun testAllocateMemoryUnrounded7() {

        // memory + offset + size unrounded
        program!!.initMem(ByteArray(32))
        val offset = 16
        val size = 16
        program!!.allocateMemory(offset, size)
        assertEquals(32, program!!.getMemSize())
    }


}