package io.lunarchain.lunarcoin.sync

import io.lunarchain.lunarcoin.core.Block
import io.lunarchain.lunarcoin.core.BlockChain
import io.lunarchain.lunarcoin.core.BlockChainManager
import io.lunarchain.lunarcoin.network.Peer
import org.slf4j.LoggerFactory

enum class SyncStatus {
    NOT_STARTED,
    INIT_SYNC_GET_HEADERS,
    INIT_SYNC_GET_BLOCKS,
    INIT_SYNC_COMPLETED
}

class SyncManager(val manager: BlockChainManager, val blockChain: BlockChain) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    var syncStatus: SyncStatus = SyncStatus.NOT_STARTED

    fun initSyncGetHeaders(peer: Peer) {
        syncStatus = SyncStatus.INIT_SYNC_GET_HEADERS
        peer.sendGetBlockHeaders(blockChain.getBestBlock().height + 1, 10)
    }

    fun initSyncGetBlocks(peer: Peer) {
        syncStatus = SyncStatus.INIT_SYNC_GET_BLOCKS
        peer.sendGetBlocks(blockChain.getBestBlock().height + 1, 10)
    }

    /**
     * 处理Peer同步的区块。
     */
    fun processSyncBlocks(peer: Peer, blocks: List<Block>) {
        /**
         * 同步区块中。。。
         */
        if (syncStatus == SyncStatus.INIT_SYNC_GET_BLOCKS) {

            /**
             * 收到区块的数量大于0则保存区块，否则说明同步完成，停止区块同步。
             */
            if (blocks.isNotEmpty()) {
                blocks.forEach { blockChain.importBlock(it) }

                // 继续请求区块数据，直至同步完毕。
                initSyncGetBlocks(peer)
            } else {
                syncStatus = SyncStatus.INIT_SYNC_COMPLETED

                manager.startMining()
                logger.debug("Init block sync completed")
            }
        }
    }
}