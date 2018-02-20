package io.lunarchain.lunarcoin.util

import io.lunarchain.lunarcoin.config.Constants.MINIMUM_DIFFICULTY
import io.lunarchain.lunarcoin.core.Block
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger
import java.nio.ByteBuffer
import kotlin.math.max

object BlockChainUtil {
    /**
     * 区块合法性验证，再次执行哈希算法并与target值做对比。
     */
    fun validateBlock(
        block: Block,
        parentTime: Long,
        parentDifficulty: Long
    ): Boolean {
        val headerBuffer = ByteBuffer.allocate(4 + 32 + 32 + 8 + 8 + 4)
        val ver = block.version
        val parentHash = block.parentHash
        val merkleRoot = block.trxTrieRoot
        val time = (block.time.millis / 1000) // Current timestamp as seconds since 1970-01-01T00:00 UTC
        val difficulty = BlockChainUtil.getCurrentDifficulty(
            block.height, time, parentTime, parentDifficulty
        )
        val nonce = block.nonce

        val target =
            BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16).divide(
                difficulty.toBigInteger()
            )
        val targetStr = "%064x".format(target)

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

        return hit < targetStr
    }

    fun getCurrentDifficulty(
        blockNumber: Long, blockTime: Long, parentTime: Long,
        parentDifficulty: Long
    ): Long {
        val difficulty = calculateDifficulty(blockNumber, parentTime, blockTime, parentDifficulty)

        return if (difficulty < MINIMUM_DIFFICULTY) MINIMUM_DIFFICULTY else difficulty
    }

    fun calculateDifficulty(
        blockNumber: Long,
        parentTime: Long,
        time: Long,
        parentDifficulty: Long
    ): Long {
        // https://github.com/ethereum/EIPs/blob/master/EIPS/eip-2.mediawiki
        // algorithm:
        // diff = (parent_diff +
        //         (parent_diff / 2048 * max(1 - (block_timestamp - parent_timestamp) // 10, -99))
        //        ) + 2^(periodCount - 2)

        val x = 1 - (time - parentTime) / 10
        val m = max(x, -99)

        //Sub-formula B - The difficulty bomb part, which increases the difficulty exponentially every 100,000 blocks.
        val bomb = Math.pow(2.toDouble(), ((blockNumber / 100000) - 2).toDouble()).toLong()

        return parentDifficulty + parentDifficulty / 2048 * m + bomb
    }
}