package lunar.vm.program

import io.lunarchain.lunarcoin.util.ByteUtil.EMPTY_BYTE_ARRAY
import io.lunarchain.lunarcoin.util.ByteUtil.oneByteToHexString
import lunar.vm.DataWord
import lunar.vm.program.listener.ProgramListener
import lunar.vm.program.listener.ProgramListenerAware
import java.lang.Math.ceil
import java.lang.Math.min
import java.lang.String.format
import java.util.*

class Memory: ProgramListenerAware {

    private val CHUNK_SIZE = 1024
    private val WORD_SIZE = 32

    private val chunks = LinkedList<ByteArray>()
    private var softSize: Int = 0
    private var programListener: ProgramListener? = null

    override fun setProgramListener(listener: ProgramListener) {
        this.programListener = listener
    }

    fun read(address: Int, size: Int): ByteArray {
        if (size <= 0) return EMPTY_BYTE_ARRAY

        extend(address, size)
        val data = ByteArray(size)

        var chunkIndex = address / CHUNK_SIZE
        var chunkOffset = address % CHUNK_SIZE

        var toGrab = data.size
        var start = 0

        while (toGrab > 0) {
            val copied = grabMax(chunkIndex, chunkOffset, toGrab, data, start)

            // read next chunk from the start
            ++chunkIndex
            chunkOffset = 0

            // mark remind
            toGrab -= copied
            start += copied
        }

        return data
    }

    fun write(address: Int, data: ByteArray, dataSize: Int, limited: Boolean) {
        var dataSize = dataSize

        if (data.size < dataSize)
            dataSize = data.size

        if (!limited)
            extend(address, dataSize)

        var chunkIndex = address / CHUNK_SIZE
        var chunkOffset = address % CHUNK_SIZE

        var toCapture = 0
        if (limited)
            toCapture = if (address + dataSize > softSize) softSize - address else dataSize
        else
            toCapture = dataSize

        var start = 0
        while (toCapture > 0) {
            val captured = captureMax(chunkIndex, chunkOffset, toCapture, data, start)

            // capture next chunk
            ++chunkIndex
            chunkOffset = 0

            // mark remind
            toCapture -= captured
            start += captured
        }

        programListener?.onMemoryWrite(address, data, dataSize)
    }


    fun extendAndWrite(address: Int, allocSize: Int, data: ByteArray) {
        extend(address, allocSize)
        write(address, data, data.size, false)
    }

    fun extend(address: Int, size: Int) {
        if (size <= 0) return

        val newSize = address + size

        var toAllocate = newSize - internalSize()
        if (toAllocate > 0) {
            addChunks(ceil(toAllocate.toDouble() / CHUNK_SIZE).toInt())
        }

        toAllocate = newSize - softSize
        if (toAllocate > 0) {
            toAllocate = ceil(toAllocate.toDouble() / WORD_SIZE).toInt() * WORD_SIZE
            softSize += toAllocate

            programListener?.onMemoryExtend(toAllocate)
        }
    }

    fun readWord(address: Int): DataWord {
        return DataWord(read(address, 32))
    }

    // just access expecting all data valid
    fun readByte(address: Int): Byte {

        val chunkIndex = address / CHUNK_SIZE
        val chunkOffset = address % CHUNK_SIZE

        val chunk = chunks[chunkIndex]

        return chunk[chunkOffset]
    }

    override fun toString(): String {

        val memoryData = StringBuilder()
        val firstLine = StringBuilder()
        val secondLine = StringBuilder()

        for (i in 0 until softSize) {

            val value = readByte(i)

            // Check if value is ASCII
            val character = if (0x20.toByte() <= value && value <= 0x7e.toByte()) String(byteArrayOf(value)) else "?"
            firstLine.append(character).append("")
            secondLine.append(oneByteToHexString(value)).append(" ")

            if ((i + 1) % 8 == 0) {
                val tmp = format("%4s", Integer.toString(i - 7, 16)).replace(" ", "0")
                memoryData.append("").append(tmp).append(" ")
                memoryData.append(firstLine).append(" ")
                memoryData.append(secondLine)
                if (i + 1 < softSize) memoryData.append("\n")
                firstLine.setLength(0)
                secondLine.setLength(0)
            }
        }

        return memoryData.toString()
    }

    fun size(): Int {
        return softSize
    }

    fun internalSize(): Int {
        return chunks.size * CHUNK_SIZE
    }

    fun getChunks(): List<ByteArray> {
        return LinkedList(chunks)
    }

    private fun captureMax(chunkIndex: Int, chunkOffset: Int, size: Int, src: ByteArray, srcPos: Int): Int {

        val chunk = chunks[chunkIndex]
        val toCapture = min(size, chunk.size - chunkOffset)

        System.arraycopy(src, srcPos, chunk, chunkOffset, toCapture)
        return toCapture
    }

    private fun grabMax(chunkIndex: Int, chunkOffset: Int, size: Int, dest: ByteArray, destPos: Int): Int {

        val chunk = chunks[chunkIndex]
        val toGrab = min(size, chunk.size - chunkOffset)

        System.arraycopy(chunk, chunkOffset, dest, destPos, toGrab)

        return toGrab
    }

    private fun addChunks(num: Int) {
        for (i in 0 until num) {
            chunks.add(ByteArray(CHUNK_SIZE))
        }
    }
}