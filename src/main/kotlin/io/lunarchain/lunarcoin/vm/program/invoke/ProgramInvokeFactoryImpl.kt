package io.lunarchain.lunarcoin.vm.program.invoke

import io.lunarchain.lunarcoin.core.Block
import io.lunarchain.lunarcoin.core.Transaction
import io.lunarchain.lunarcoin.storage.Repository
import io.lunarchain.lunarcoin.util.ByteUtil
import lunar.vm.DataWord
import lunar.vm.program.Program
import lunar.vm.program.invoke.ProgramInvoke
import java.math.BigInteger

class ProgramInvokeFactoryImpl: ProgramInvokeFactory {

    override fun createProgramInvoke(tx: Transaction, block: Block, blockStore: Repository): ProgramInvoke {
        /***         ADDRESS op       ***/
        // YP: Get address of currently executing account.
        val address = if (tx.isContractCreation()) tx.getContractAddress() else tx.receiverAddress

        /***         ORIGIN op       ***/
        // YP: This is the sender of original transaction; it is never a contract.
        val origin = tx.getSender()

        /***         CALLER op       ***/
        // YP: This is the address of the account that is directly responsible for this execution.
        val caller = tx.getSender()

        /***         BALANCE op       ***/
        val balance = blockStore.getBalance(address!!).toByteArray()

        /***         GASPRICE op       ***/
        val gasPrice = tx.gasPrice

        /*** GAS op ***/
        val gas = tx.gasLimit

        /***        CALLVALUE op      ***/
        val callValue = tx.amount.toByteArray()

        /***     CALLDATALOAD  op   ***/
        /***     CALLDATACOPY  op   ***/
        /***     CALLDATASIZE  op   ***/
        val data = if (tx.isContractCreation()) ByteUtil.EMPTY_BYTE_ARRAY else tx.data

        /***    PREVHASH  op  ***/
        val lastHash = block.parentHash

        /***   COINBASE  op ***/
        val coinbase = block.coinBase

        /*** TIMESTAMP  op  ***/
        val timestamp = block.time

        /*** NUMBER  op  ***/
        val number = block.height

        /*** DIFFICULTY  op  ***/
        val difficulty = block.difficulty

        /*** GASLIMIT op ***/
        val gaslimit = block.gasLimit

        return ProgramInvokeImpl(
            address, origin!!, caller!!, balance, gasPrice, gas, callValue, data,
            lastHash, coinbase, timestamp.millis, number, difficulty.toBigInteger().toByteArray(), gaslimit,
            blockStore, false
        )
    }

    override fun createProgramInvoke(
        program: Program,
        toAddress: DataWord,
        callerAddress: DataWord,
        inValue: DataWord,
        inGas: DataWord,
        balanceInt: BigInteger,
        dataIn: ByteArray,
        blockStore: Repository,
        staticCall: Boolean,
        byTestingSuite: Boolean
    ): ProgramInvoke {
        val address = toAddress
        val origin = program.getOriginAddress()
        val caller = callerAddress

        val balance = DataWord(balanceInt.toByteArray())
        val gasPrice = program.getGasPrice()
        val gas = inGas
        val callValue = inValue

        val data = dataIn
        val lastHash = program.getPrevHash()
        val coinbase = program.getCoinbase()
        val timestamp = program.getTimestamp()
        val number = program.getNumber()
        val difficulty = program.getDifficulty()
        val gasLimit = program.getGasLimit()

        return ProgramInvokeImpl(
            address, origin, caller, balance, gasPrice, gas, callValue,
            data, lastHash, coinbase, timestamp, number, difficulty, gasLimit,
            blockStore, program.getCallDeep() + 1, staticCall, byTestingSuite
        )

    }
}