package io.lunarchain.lunarcoin.core

import io.lunarchain.lunarcoin.config.BlockChainConfig
import io.lunarchain.lunarcoin.config.Constants.BLOCK_REWARD
import io.lunarchain.lunarcoin.storage.BlockInfo
import io.lunarchain.lunarcoin.storage.MemoryDataSource
import io.lunarchain.lunarcoin.storage.Repository
import io.lunarchain.lunarcoin.trie.PatriciaTrie
import io.lunarchain.lunarcoin.util.BlockChainUtil
import io.lunarchain.lunarcoin.util.CodecUtil
import io.lunarchain.lunarcoin.vm.program.invoke.ProgramInvokeFactory
import io.lunarchain.lunarcoin.vm.program.invoke.ProgramInvokeFactoryImpl
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger
import java.util.*

/**
 * 区块链(BlockChain)，一个BlockChain实例就代表一个链。
 */
class BlockChain(val config: BlockChainConfig, val repository: Repository) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private var bestBlock: Block = config.getGenesisBlock()

    //TODO remove hard-code
    private val gasPrice = 100.toBigInteger()

    private val gasLimit = 1000.toBigInteger()

    /**
     * 交易处理实例。
     */
    //val transactionExecutor = TransactionExecutor(repository)

    init {
        // 检查NodeId，如果不存在就自动生成NodeId。
        config.getNodeId()

        loadBestBlock()
    }

    /**
     * 读取BestBlock
     */
    private fun loadBestBlock(): Block {
        bestBlock = repository.getBestBlock() ?: config.getGenesisBlock()
        repository.changeAccountStateRoot(bestBlock.stateRoot)
        logger.debug("Best block is:" + bestBlock)
        return bestBlock
    }

    fun getBestBlock() = bestBlock

    fun updateBestBlock(newBlock: Block) {
        logger.debug("Updating best block to ${Hex.toHexString(newBlock.hash)}")
        repository.changeAccountStateRoot(newBlock.stateRoot)
        repository.updateBestBlock(newBlock)
        this.bestBlock = newBlock
    }

    /**
     * 构造新的区块，要素信息为：区块高度(height)，父区块的哈希值(parentHash), 交易记录(transactions)，时间戳(time)。
     * 新的区块不会影响当前区块链的状态。
     */
    fun generateNewBlock(parent: Block, transactions: List<Transaction>): Block {
        val coinbaseTransaction = generateCoinBaseTransaction()
        val transactionsToInclude = listOf(coinbaseTransaction) + transactions

        val block = Block(
            config.getPeerVersion(), parent.height + 1, parent.hash,
            config.getMinerCoinbase(), DateTime(), 0, 0, parent.totalDifficulty,
            ByteArray(0), calculateTrxTrieRoot(transactionsToInclude),
            transactionsToInclude,
            parent.gasLimit
        )
        return block
    }

    fun calculateTrxTrieRoot(transactions: List<Transaction>): ByteArray {
        val trie = PatriciaTrie(MemoryDataSource("trxTrieRoot"))
        if (transactions.size > 0) {
            for (i in 0..transactions.size - 1) {
                trie.update(i.toString().toByteArray(), CodecUtil.encodeTransaction(transactions[i]))
            }
        }
        return trie.rootHash
    }

    /**
     * 构造CoinBase Transaction (https://bitcoin.org/en/glossary/coinbase-transaction)，矿工的收益。
     */
    private fun generateCoinBaseTransaction(): Transaction {
        return Transaction(
            COINBASE_SENDER_ADDRESS,
            config.getMinerCoinbase(), BLOCK_REWARD, DateTime(), config.getNodePubKey()!!, ByteArray(0), ByteArray(0),
            gasPrice.toByteArray(), gasLimit.toByteArray(), ByteArray(0)
        )
    }

    /**
     * 执行区块的交易数据，会影响当前区块链的状态。
     *
     * TODO: 费用的计算和分配。
     */
    fun processBlock(block: Block): Block {
        var executor = TransactionExecutor(repository, bestBlock, block.transactions[0], 0L, repository, ProgramInvokeFactoryImpl())
        var totalGasUsed: Long = 0

        repository.startTracking()

        try {
            executor.executeCoinbaseTransaction(block.transactions[0])
            repository.putTransaction(block.transactions[0])

            for (trx in block.transactions.drop(1)) {
                executor = TransactionExecutor(repository, bestBlock, trx, totalGasUsed, repository, ProgramInvokeFactoryImpl())
                //repository.startTracking()
                executor.init()
                executor.execute()
                executor.go()
                totalGasUsed += executor.getGasUsed()
            }
        }  catch (e: Exception) {
            repository.rollback()
        } finally {
        }
        repository.commit()

        return Block(
            block.version, block.height, block.parentHash, block.coinBase,
            block.time, block.difficulty, block.nonce, block.totalDifficulty,
            repository.getAccountStateRoot() ?: ByteArray(0), block.trxTrieRoot,
            block.transactions,
            block.gasLimit
        )
    }

    /**
     * Import区块数据的结果
     */
    enum class ImportResult {

        BEST_BLOCK,
        NON_BEST_BLOCK,
        EXIST,
        NO_PARENT,
        INVALID_BLOCK
    }

    /**
     * 保存区块数据。
     *
     * TODO: 实现AccountState的Merkle Patricia Tree存储。
     */
    fun importBlock(block: Block): ImportResult {
        // Validate Block
        if (!BlockChainUtil.validateBlock(block, bestBlock.time.millis / 1000, bestBlock.difficulty)) {
            logger.debug("Invalid block: $block, will not do import")
            return ImportResult.INVALID_BLOCK
        }

        if (block.height < bestBlock.height && repository.getBlock(block.hash) != null) {
            return ImportResult.EXIST
        }

        if (isNextBlock(block)) {
            val processedBlock = processAndSaveBlock(block)

            updateMainBlockInfo(processedBlock)
            updateBestBlock(processedBlock)

            return ImportResult.BEST_BLOCK
        } else {
            if (repository.getBlock(block.parentHash) != null) { // Fork
                logger.debug("Fork block $block.")

                return forkBlock(block)
            } else {
                logger.debug("Discard block without parent block $block.")

                return ImportResult.NO_PARENT
            }

        }
    }

    private fun processAndSaveBlock(block: Block): Block {
        val blockToSave = processBlock(block)
        logger.debug("Push block $blockToSave to end of chain.")

        repository.saveBlock(blockToSave)

        logger.debug("Block hash: ${Hex.toHexString(blockToSave.hash)}")

        return blockToSave
    }

    private fun forkBlock(block: Block): ImportResult {
        val parentBlockHash = block.parentHash
        var parentBlock = repository.getBlock(parentBlockHash) ?: return ImportResult.NO_PARENT

        // 1. 保存当前StateRoot
        val oldBestBlock = bestBlock

        // 2. 切换StateRoot到父区块
        repository.changeAccountStateRoot(parentBlock.stateRoot)

        // 3. 处理并保存Block
        val blockProcessed = processAndSaveBlock(block)

        // 4. 根据总难度来判断是否需要切换主分支
        if (blockProcessed.totalDifficulty > oldBestBlock.totalDifficulty) { // 切换主分支
            rebranch(blockProcessed, oldBestBlock)
            updateBestBlock(blockProcessed)
        } else { // 不需要分叉，把新区块作为分支记录下来
            updateBranchBlockInfo(blockProcessed)
            updateBestBlock(oldBestBlock)
        }

        return ImportResult.BEST_BLOCK
    }

    private fun rebranch(newBestBlock: Block, oldBestBlock: Block){
        // 1. 主链与新链先退至同一高度
        var newLine = newBestBlock
        if (newBestBlock.height > oldBestBlock.height) {
            while(newLine.height > oldBestBlock.height) {
                updateMainBlockInfo(newLine)
                newLine = repository.getBlock(newLine.parentHash)!!
            }
        }

        var oldLine = oldBestBlock
        if (oldBestBlock.height > newBestBlock.height) {
            while(oldLine.height > newBestBlock.height) {
                updateBranchBlockInfo(oldLine)
                oldLine = repository.getBlock(oldLine.parentHash)!!
            }
        }

        // 2. 退至公共区块
        while(!newLine.hash.contentEquals(oldLine.hash)) {
            updateMainBlockInfo(newLine)
            newLine = repository.getBlock(newLine.parentHash)!!
            updateBranchBlockInfo(oldLine)
            oldLine = repository.getBlock(oldLine.parentHash)!!
        }
    }

    private fun updateMainBlockInfo(block: Block) {
        val blockInfo = BlockInfo(block.hash, true, block.totalDifficulty)

        repository.updateBlockInfo(block.height, blockInfo)
    }

    private fun updateBranchBlockInfo(block: Block) {
        val blockInfo = BlockInfo(block.hash, false, block.totalDifficulty)

        repository.updateBlockInfo(block.height, blockInfo)
    }

    private fun isNextBlock(block: Block): Boolean {
        val isNext = Arrays.equals(bestBlock.hash, block.parentHash)
        if (isNext) {
            logger.debug("Parent of block is same as best block.")
        } else {
            logger.debug("Parent of block is DIFFERENT FROM best block.")
            logger.debug("Best block Hash:" + Hex.toHexString(bestBlock.hash))
            logger.debug("Parent block Hash:" + Hex.toHexString(block.parentHash))
        }
        return isNext
    }

}
