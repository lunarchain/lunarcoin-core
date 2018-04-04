package io.lunarchain.lunarcoin.tests

import io.lunarchain.lunarcoin.config.BlockChainConfig
import io.lunarchain.lunarcoin.core.*
import io.lunarchain.lunarcoin.network.client.PeerClient
import io.lunarchain.lunarcoin.network.server.PeerServer
import io.lunarchain.lunarcoin.storage.Repository
import io.lunarchain.lunarcoin.storage.ServerRepository
import io.lunarchain.lunarcoin.util.CodecUtil
import io.lunarchain.lunarcoin.util.CryptoUtil
import io.lunarchain.lunarcoin.util.HashUtil
import io.lunarchain.lunarcoin.vm.program.invoke.ProgramInvokeFactoryImpl
import org.joda.time.DateTime
import org.junit.Test
import java.lang.Thread.sleep
import java.math.BigInteger

class NetworkTest {

    val config = BlockChainConfig.default()

    @Test
    fun peerClientServerTest() {
        val serverConfig = BlockChainConfig("application-1.conf")

        val clientConfig = BlockChainConfig("application-2.conf")

        val serverRepository = ServerRepository.getInstance(serverConfig)
        val clientRepository = ServerRepository.getInstance(clientConfig)

        val serverChain = BlockChain(serverConfig, serverRepository)

        val clientChain = BlockChain(clientConfig, clientRepository)

        val serverManager = BlockChainManager(serverChain)

        val clientManager = BlockChainManager(clientChain)

        val server = PeerServer(serverManager)
        Thread(Runnable {
            server.start()
        }).start()

        val newTrx = newTransaction(serverRepository)

        serverManager.submitTransaction(newTrx)

        serverManager.startMining()

        sleep(90 * 1000)

        val client = PeerClient(clientManager)
        Thread(Runnable {
            client.connectAsync(Node("", "localhost", config.getPeerListenPort()))
        }).start()

        sleep(90 * 1000)

        server.closeAsync()

        client.closeAsync()

        sleep(10000)
    }

    fun newTransaction(repo: Repository): Transaction {
        // 初始化Alice账户
        val kp1 = CryptoUtil.generateKeyPair()
        val alice = Account(kp1.public)

        // 初始化Bob账户
        val kp2 = CryptoUtil.generateKeyPair()
        val bob = Account(kp2.public)


        repo.createAccountState(alice.address)
        repo.createAccountState(bob.address)

        // Alice向Bob转账100

        val trx = Transaction(alice.address, bob.address, BigInteger.valueOf(100), DateTime(), kp1.public, ByteArray(0),
            (repo.getAccountState(alice.address)!!.nonce).toByteArray(), 1.toBigInteger().toByteArray(), 100.toBigInteger().toByteArray(), ByteArray(0))

        val transactionExecutor = TransactionExecutor(repo, config.getGenesisBlock(), trx, 0L, repo, ProgramInvokeFactoryImpl())

        // 初始金额为200
        transactionExecutor.addBalance(alice.address, BigInteger.valueOf(200))
        transactionExecutor.addBalance(bob.address, BigInteger.valueOf(200))

        // Alice用私钥签名
        trx.sign(kp1.private)
        return trx
    }

}
