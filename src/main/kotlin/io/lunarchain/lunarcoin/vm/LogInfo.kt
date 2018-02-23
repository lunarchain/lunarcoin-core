package io.lunarchain.lunarcoin.vm

import lunar.vm.DataWord
import java.util.ArrayList

class LogInfo() {
    private var address = byteArrayOf()
    private var topics: MutableList<DataWord>? = ArrayList()
    private var data = byteArrayOf()

    constructor(address: ByteArray, topics: MutableList<DataWord>, data: ByteArray): this() {
        this.address = address ?: byteArrayOf()
        this.topics = topics ?: ArrayList<DataWord>()
        this.data = data ?: byteArrayOf()
    }

}