package io.lunarchain.lunarcoin.vm.program

import io.lunarchain.lunarcoin.core.Transaction
import io.lunarchain.lunarcoin.util.ByteUtil
import lunar.vm.DataWord
import org.joda.time.DateTime
import java.math.BigInteger
import java.security.PublicKey

class InternalTransaction(senderAddress: ByteArray, receiverAddress: ByteArray, amount: BigInteger,
                          time: DateTime, publicKey: PublicKey, signature: ByteArray = ByteArray(0),
                          val parentHash: ByteArray, val deep: Int, val index: Int, nonce: ByteArray, gasPrice: DataWord,
                          gasLimit: DataWord, val value: ByteArray, data: ByteArray, val note: String, var rejected: Boolean = false)
    : Transaction(senderAddress, receiverAddress, amount, time, publicKey, ByteArray(0), nonce, gasPrice.getData(), gasLimit.getData(), data) {

    private fun getData(gasPrice: DataWord?): ByteArray {
        return gasPrice?.getData() ?: ByteUtil.EMPTY_BYTE_ARRAY
    }

    fun reject() {
        this.rejected = true
    }



}