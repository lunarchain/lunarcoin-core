package io.lunarchain.lunarcoin.miner

import io.lunarchain.lunarcoin.core.Block
import io.lunarchain.lunarcoin.util.BlockChainUtil
import io.lunarchain.lunarcoin.util.CryptoUtil
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger
import java.nio.ByteBuffer


/**
 * 挖矿的管理类，算法可以参照https://en.bitcoin.it/wiki/Block_hashing_algorithm。
 */
object BlockMiner {

    private val logger = LoggerFactory.getLogger(javaClass)

    var working = false

    /**
     * 挖矿，返回nonce值和target值。目前采用阻塞模型，后期修改为更合理的异步模型。
     */
    fun mine(block: Block, parentTime: Long, parentDifficulty: Long): MineResult {
        logger.info("Miner is working ...")

        working = true

        val startTime = System.currentTimeMillis()

        val ver = block.version
        val parentHash = block.parentHash
        val merkleRoot = block.trxTrieRoot
        val time = (block.time.millis / 1000) // Current timestamp as seconds since 1970-01-01T00:00 UTC
        val difficulty = BlockChainUtil.getCurrentDifficulty(
            block.height, time, parentTime,
            parentDifficulty
        )

        // 挖矿难度的算法：https://en.bitcoin.it/wiki/Difficulty
        val target =
            BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16).divide(
                difficulty.toBigInteger()
            )
        val targetStr = "%064x".format(target)

        var nonce = 0
        while (working && nonce < 0x100000000) {

            val headerBuffer = ByteBuffer.allocate(4 + 32 + 32 + 8 + 8 + 4)
            headerBuffer.put(ByteBuffer.allocate(4).putInt(ver).array()) // version
            headerBuffer.put(parentHash) // parentHash
            headerBuffer.put(merkleRoot) // trxTrieRoot
            headerBuffer.put(ByteBuffer.allocate(8).putLong(time).array()) // time
            headerBuffer.put(
                ByteBuffer.allocate(8).putLong(difficulty).array()
            ) // difficulty(current difficulty)
            headerBuffer.put(ByteBuffer.allocate(4).putInt(nonce).array()) // nonce

            val header = headerBuffer.array()
            val hit = Hex.toHexString(CryptoUtil.sha256(CryptoUtil.sha256(header)))

            if (hit < targetStr) {
                break
            }
            nonce += 1
        }

        if (working) {
            val endTime = System.currentTimeMillis()

            val interval = (endTime - startTime) / 1000

            val totalDifficulty = block.totalDifficulty + BigInteger.valueOf(difficulty.toLong())

            val newBlock = Block(
                block.version, block.height, block.parentHash, block.coinBase,
                block.time, difficulty, nonce, totalDifficulty, block.stateRoot, block.trxTrieRoot,
                block.transactions
            )
            logger.info("Mined block $newBlock in $interval seconds.")

            val result = MineResult(true, difficulty, nonce, newBlock)

            working = false

            return result
        } else {
            val result = MineResult(false, difficulty, nonce, block)

            return result
        }
    }

    fun skip() {
        logger.info("Skip mining current block...")
        working = false
    }

    fun stop() {
        working = false
    }
}
