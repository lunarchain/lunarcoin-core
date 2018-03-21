package io.lunarchain.lunarcoin.vm

class CallCreate(private val data: ByteArray, private val destination: ByteArray, private val gasLimit: ByteArray, private val value: ByteArray) {


    fun getData(): ByteArray {
        return data!!
    }

    fun getDestination(): ByteArray {
        return destination!!
    }

    fun getGasLimit(): ByteArray {
        return gasLimit!!
    }

    fun getValue(): ByteArray {
        return value!!
    }
}