package io.lunarchain.lunarcoin.core

import io.lunarchain.lunarcoin.storage.Repository
import io.lunarchain.lunarcoin.util.BIUtil
import io.lunarchain.lunarcoin.util.BIUtil.isNotEqual
import io.lunarchain.lunarcoin.util.BIUtil.toBI
import io.lunarchain.lunarcoin.util.ByteUtil.EMPTY_BYTE_ARRAY
import io.lunarchain.lunarcoin.util.FastByteComparisons
import io.lunarchain.lunarcoin.util.HashUtil.EMPTY_DATA_HASH
import io.lunarchain.lunarcoin.vm.PrecompiledContracts
import io.lunarchain.lunarcoin.vm.program.invoke.ProgramInvokeFactory
import lunar.vm.DataWord
import lunar.vm.GasCost
import lunar.vm.VM
import lunar.vm.program.Program
import lunar.vm.program.ProgramResult
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex
import java.lang.reflect.Array.getLength
import java.math.BigInteger

/**
 * 交易处理并更新账户状态。
 */
class TransactionExecutor(val repository: Repository, val bestBlock: Block, val tx: Transaction,
                          val gasUsedInTheBlock: Long, val track: Repository, val programInvokeFactory: ProgramInvokeFactory) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private var execError: String? = null

    private var m_endGas = BigInteger.ZERO

    private var localCall = false

    private var readyToExecute = false

    //TODO remove hard-code
    private val basicTxCost = 0L

    private var precompiledContract: PrecompiledContracts.PrecompiledContract? = null

    private var result: ProgramResult? = ProgramResult()

    private var vm: VM? = null

    private var program: Program? = null

    private fun execError(err: String) {
        logger.warn(err)
        execError = err
    }


    /**
     * 增加账户余额(balance)，如果amount为负数则余额减少。
     */
    fun addBalance(address: ByteArray, amount: BigInteger) {
        repository.addBalance(address, amount)
    }

    /**
     * 转账功能，发送方减少金额，接收方增加金额。
     */
    fun transfer(fromAddress: ByteArray, toAddress: ByteArray, amount: BigInteger) {
        addBalance(fromAddress, amount.negate())
        addBalance(toAddress, amount)
    }

    /**
     * Coinbase Reward，coinbaseAddress增加金额。
     */
    fun coinbaseTransfer(coinbaseAddress: ByteArray, amount: BigInteger) {
        addBalance(coinbaseAddress, amount)
    }

    /**
     * 根据交易记录更新区块链的状态(state)，发送方的余额会减少，接收方的余额会增加。
     * 区块链的状态更新应该是原子操作，持久层是数据库可以使用事务功能。
     */
    fun execute() {

        if (!readyToExecute) return

        if (!localCall) {
            track.increaseNonce(tx.senderAddress)

            val txGasLimit = toBI(tx.gasLimit)
            val txGasCost = toBI(tx.gasPrice).multiply(txGasLimit)
            track.addBalance(tx.senderAddress, txGasCost.negate())

            if (logger.isInfoEnabled)
                logger.info(
                    "Paying: txGasCost: [{}], gasPrice: [{}], gasLimit: [{}]",
                    txGasCost,
                    toBI(tx.gasPrice),
                    txGasLimit
                )
        }

        if (tx.isContractCreation()) {
            create()
        } else {
            call()
        }
    }

    fun create() {
        val newContractAddress = tx.getContractAddress()
        val existingAddr = track.getAccountState(newContractAddress!!)
        if (existingAddr != null) {
            execError(
                "Trying to create a contract with existing contract address: 0x" + Hex.toHexString(
                    newContractAddress
                )
            )
            m_endGas = BigInteger.ZERO
            return
        }

        //In case of hashing collisions (for TCK tests only), check for any balance before createAccount()
        val oldBalance = track.getBalance(newContractAddress)
        track.getOrCreateAccountState(tx.getContractAddress()!!)
        track.addBalance(newContractAddress, oldBalance)
        track.increaseNonce(newContractAddress)


        if (tx.data.isEmpty()) {
            m_endGas = m_endGas.subtract(BigInteger.valueOf(basicTxCost))
            result!!.spendGas(basicTxCost)
        } else {
            val programInvoke =  programInvokeFactory.createProgramInvoke(tx, bestBlock, track)
            this.vm = VM
            this.program = Program(tx.data, programInvoke, tx)
        }

        val endowment = tx.amount

        transfer(tx.getSender()!!, newContractAddress, endowment)


    }

    fun call() {
        if (!readyToExecute) return
        val targetAddress = tx.receiverAddress
        precompiledContract = PrecompiledContracts.getContractForAddress(DataWord(targetAddress))

        if (precompiledContract != null) {
            val requiredGas = precompiledContract!!.getGasForData(tx.data)
            val spendingGas = BigInteger.valueOf(requiredGas).add(BigInteger.valueOf(basicTxCost))
            if (!localCall && m_endGas.compareTo(spendingGas) < 0) {
                 // no refund
                // no endowment
                execError(
                    "Out of Gas calling precompiled contract 0x" + Hex.toHexString(targetAddress) + ", required: " + spendingGas + ", left: " + m_endGas
                )
                m_endGas = BigInteger.ZERO
                return
            } else {

                m_endGas = m_endGas.subtract(spendingGas)

                // FIXME: save return for vm trace
                val out = precompiledContract!!.execute(tx.data)

                if (!out.first) {
                    execError("Error executing precompiled contract 0x" + Hex.toHexString(targetAddress))
                    m_endGas = BigInteger.ZERO
                    return
                }
            }
        } else {
            val code = track.getCode(targetAddress)
            if (code!!.isEmpty())  {
                m_endGas = m_endGas.subtract(BigInteger.valueOf(basicTxCost))
                result!!.spendGas(basicTxCost)
            } else {
                val programInvoke =  programInvokeFactory.createProgramInvoke(tx, bestBlock, track);

                this.vm = VM
                this.program = Program(track.getCodeHash(targetAddress)!!, code, programInvoke, tx)

            }
        }

        val endowment = tx.amount
        transfer(tx.senderAddress, targetAddress, endowment)
    }

    fun go() {

        if (!readyToExecute) return

        try {
            if (vm != null) {
                // Charge basic cost of the transaction
                program!!.spendGas(basicTxCost, "TRANSACTION COST")
                vm!!.play(program!!)
                result = program!!.getResult()
                m_endGas = toBI(tx.gasLimit).subtract(toBI(program!!.getResult().getGasUsed()))
                if (tx.isContractCreation() && !result!!.isRevert()) {
                    val returnDataGasValue = getLength(program!!.getResult().getHReturn()) * GasCost().getCREATE_DATA()

                    if (m_endGas.compareTo(BigInteger.valueOf(returnDataGasValue.toLong())) < 0) {
                        program!!.setRuntimeFailure(
                            Program.notEnoughSpendingGas(
                                "No gas to return just created contract",
                                returnDataGasValue.toLong(), program!!
                            )
                        )
                        result = program!!.getResult()
                        result!!.setHReturn(EMPTY_BYTE_ARRAY)
                    } else if (getLength(result!!.getHReturn()) > Integer.MAX_VALUE) {
                        program!!.setRuntimeFailure(
                            Program.notEnoughSpendingGas(
                                "Contract size too large: " + getLength(result!!.getHReturn()),
                                returnDataGasValue.toLong(), program!!
                            )
                        )
                        result = program!!.getResult()
                        result!!.setHReturn(EMPTY_BYTE_ARRAY)
                    } else {
                        // Contract successfully created
                        m_endGas = m_endGas.subtract(BigInteger.valueOf(returnDataGasValue.toLong()))
                        track.saveCode(tx.getContractAddress()!!, result!!.getHReturn())
                    }
                }

                if (result!!.getException() != null || result!!.isRevert()) {
                    result!!.getDeleteAccounts().clear()
                    result!!.getLogInfoList().clear()
                    result!!.resetFutureRefund()
                    rollback()

                    if (result!!.getException() != null) {
                        throw result!!.getException()!!
                    } else {
                        execError("REVERT opcode executed")
                    }
                } else {
                    track.putTransaction(tx)
                    track.commit()
                }
            } else {
                track.putTransaction(tx)
                track.commit()
            }

        } catch (e: Throwable) {
            rollback()
            m_endGas = BigInteger.ZERO
            execError(e.message!!)
        }

    }

    fun rollback() {
        track.rollback()

    }

    /**
     * 在交易执行前做一些基本的检查
     */

    fun init() {

        if(localCall) readyToExecute = true

        val txGasLimit = BigInteger(1, tx.gasLimit)
        val curBlockGasLimit = BigInteger(1, bestBlock.gasLimit)
        val cumulativeGasReached = txGasLimit.add(BigInteger.valueOf(gasUsedInTheBlock)).compareTo(curBlockGasLimit) > 0

        if (cumulativeGasReached) {
            execError(
                String.format(
                    "Too much gas used in this block: Require: %s Got: %s",
                    BigInteger(1, bestBlock.gasLimit).toLong() - toBI(tx.gasLimit).toLong(),
                    toBI(tx.gasLimit).toLong()
                )
            )
            return
        }

        val senderAddress = tx.senderAddress

        val reqNonce = track.getNonce(senderAddress)
        val txNonce = toBI(tx.nonce)

        if (isNotEqual(reqNonce, txNonce)) {
            execError(String.format("Invalid nonce: required: %s , tx.nonce: %s", reqNonce, txNonce))

            return
        }

        val txGasCost = toBI(tx.gasPrice).multiply(txGasLimit)
        val totalCost = toBI(tx.amount.toByteArray()).add(txGasCost)
        val senderBalance = track.getBalance(tx.senderAddress)
        if (!BIUtil.isCovers(senderBalance, totalCost)) {

            execError(String.format("Not enough cash: Require: %s, Sender cash: %s", totalCost, senderBalance))

            return
        }
        //TODO 签名验证

        readyToExecute = true


    }

    /**
     * 执行Coinbase Transaction。
     */
    fun executeCoinbaseTransaction(trx: Transaction) {
        if (trx.isCoinbaseTransaction()) {
            logger.debug("Reward ${trx.amount} coins to ${Hex.toHexString(trx.receiverAddress)}")

            // Execute reward
            coinbaseTransfer(trx.receiverAddress, trx.amount)
        } else {
            throw IllegalTransactionException()
        }
    }

    fun getGasUsed(): Long {
        return toBI(tx.gasLimit).subtract(m_endGas).toLong()
    }

    fun setLocalCall(localCall: Boolean): TransactionExecutor {
        this.localCall = localCall
        return this
    }

}

/**
 * 交易内容非法（没有签名或签名错误）。
 */
class IllegalTransactionException : Throwable() {

}
