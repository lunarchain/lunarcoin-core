package io.lunarchain.lunarcoin.vm.program.invoke

import io.lunarchain.lunarcoin.core.Block
import io.lunarchain.lunarcoin.core.Transaction
import io.lunarchain.lunarcoin.storage.Repository
import lunar.vm.DataWord
import lunar.vm.program.Program
import lunar.vm.program.invoke.ProgramInvoke
import java.math.BigInteger

interface ProgramInvokeFactory {

     fun createProgramInvoke(
         tx: Transaction, block: Block,
         blockStore: Repository
    ): ProgramInvoke

     fun createProgramInvoke(
         program: Program, toAddress: DataWord, callerAddress: DataWord,
         inValue: DataWord, inGas: DataWord,
         balanceInt: BigInteger, dataIn: ByteArray,
         blockStore: Repository,
         staticCall: Boolean, byTestingSuite: Boolean
    ): ProgramInvoke
}