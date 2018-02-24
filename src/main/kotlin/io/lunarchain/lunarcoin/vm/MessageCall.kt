package io.lunarchain.lunarcoin.vm

import lunar.vm.DataWord
import lunar.vm.OpCode

class MessageCall() {
    /**
     * Type of internal call. Either CALL, CALLCODE or POST
     */
    private var type: OpCode? = null

    /**
     * gas to pay for the call, remaining gas will be refunded to the caller
     */
    private var gas: DataWord? = null
    /**
     * address of account which code to call
     */
    private var codeAddress: DataWord? = null
    /**
     * the value that can be transfer along with the code execution
     */
    private var endowment: DataWord? = null
    /**
     * start of memory to be input data to the call
     */
    private var inDataOffs: DataWord? = null
    /**
     * size of memory to be input data to the call
     */
    private var inDataSize: DataWord? = null
    /**
     * start of memory to be output of the call
     */
    private var outDataOffs: DataWord? = null
    /**
     * size of memory to be output data to the call
     */
    private var outDataSize: DataWord? = null

    constructor(type: OpCode, gas: DataWord, codeAddress: DataWord,
                endowment: DataWord, inDataOffs: DataWord, inDataSize: DataWord): this() {
        this.type = type
        this.gas = gas
        this.codeAddress = codeAddress
        this.endowment = endowment
        this.inDataOffs = inDataOffs
        this.inDataSize = inDataSize

    }

    constructor(type: OpCode, gas: DataWord, codeAddress: DataWord,
                endowment: DataWord, inDataOffs: DataWord, inDataSize: DataWord,
                outDataOffs: DataWord, outDataSize: DataWord): this(type, gas, codeAddress, endowment, inDataOffs, inDataSize) {
        this.outDataOffs = outDataOffs
        this.outDataSize = outDataSize

    }

    fun getType(): OpCode? {
        return type
    }

    fun getGas(): DataWord? {
        return gas
    }

    fun getCodeAddress(): DataWord? {
        return codeAddress
    }

    fun getEndowment(): DataWord? {
        return endowment
    }

    fun getInDataOffs(): DataWord? {
        return inDataOffs
    }

    fun getInDataSize(): DataWord? {
        return inDataSize
    }

    fun getOutDataOffs(): DataWord? {
        return outDataOffs
    }

    fun getOutDataSize(): DataWord? {
        return outDataSize
    }
}