package io.lunarchain.lunarcoin.core

import io.lunarchain.lunarcoin.config.BlockChainConfig
import io.lunarchain.lunarcoin.config.Constants.MINIMUM_DIFFICULTY
import io.lunarchain.lunarcoin.util.CodecUtil
import io.lunarchain.lunarcoin.util.CryptoUtil
import io.lunarchain.lunarcoin.util.CryptoUtil.Companion.generateKeyPair
import io.lunarchain.lunarcoin.util.CryptoUtil.Companion.sha256
import io.lunarchain.lunarcoin.util.CryptoUtil.Companion.verifyTransactionSignature
import org.joda.time.DateTime
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test
import org.spongycastle.asn1.ASN1InputStream
import org.spongycastle.asn1.util.ASN1Dump
import org.spongycastle.jce.provider.BouncyCastleProvider
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.KeyPairGenerator
import java.security.Security
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class BlockChainTest {

    val config = BlockChainConfig.default()

    @Before
    fun setup() {
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }

    @After
    fun close() {
    }

    /**
     * 验证账户地址长度为40(20个byte)。
     */
    @Test
    fun validateAddressTest() {
        val keyPair = generateKeyPair()

        val account = Account(keyPair.public)
        assert(account.address.size == 20)
    }

    /**
     * 验证ECDSAE签名算法。
     */
    @Test
    fun verifyECDSASignatureTest() {
        // Get the instance of the Key Generator with "EC" algorithm

        val gen = KeyPairGenerator.getInstance("EC", "SC")
        gen.initialize(ECGenParameterSpec("secp256r1"))

        val pair = gen.generateKeyPair()
        // Instance of signature class with SHA256withECDSA algorithm
        val signer = Signature.getInstance("SHA256withECDSA")
        signer.initSign(pair.private)

        println("Private Keys is::" + pair.private)
        println("Public Keys is::" + pair.public)

        val msg = "text ecdsa with sha256"//getSHA256(msg)
        signer.update(msg.toByteArray())

        val signature = signer.sign()
        println("Signature is::" + BigInteger(1, signature).toString(16))

        // Validation
        signer.initVerify(pair.public)
        signer.update(msg.toByteArray())
        assert(signer.verify(signature))

    }

    /**
     * 验证交易签名。
     */
    @Test
    fun verifyTransactionSignatureTest() {
        // 初始化Alice账户
        val kp1 = generateKeyPair()
        val alice = Account(kp1.public)

        // 初始化Bob账户
        val kp2 = generateKeyPair()
        val bob = Account(kp2.public)

        // Alice向Bob转账100
        val trx =
            Transaction(alice.address, bob.address, BigInteger.valueOf(100), DateTime(), kp1.public)

        // Alice用私钥签名
        val signature = trx.sign(kp1.private)

        // 用Alice的公钥验证交易签名
        assert(verifyTransactionSignature(trx, signature))

        // 验证交易的合法性(签名验证)
        assert(trx.isValid)
    }

    @Test
    fun addressTest() {
        // 初始化Alice账户
        val kp1 = generateKeyPair()
        val alice = Account(kp1.public)

        assertNotNull(alice.address)

        println(Hex.toHexString(alice.address))
    }

    /**
     * 挖矿算法测试。
     */
    @Test
    fun mineAlgorithmTest() {
        val ver: Int = 1
        val parentHash = "000000000000000117c80378b8da0e33559b5997f2ad55e2f7d18ec1975b9717"
        val merkleRoot = "871714dcbae6c8193a2bb9b2a69fe1c0440399f38d94b3a0f1b447275a29978a"
        val time = 0x53058b35L // 2014-02-20 04:57:25
        val difficulty = MINIMUM_DIFFICULTY // difficulty，比特币的最小(初始)难度为0x1d00ffff，为测试方便我们降低难度为0x1f00ffff

        // 挖矿难度的算法：https://en.bitcoin.it/wiki/Difficulty
        val target =
            BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16).divide(
                difficulty.toBigInteger()
            )
        val targetStr = "%064x".format(target)
        println("Target:$targetStr")

        var nonce = 0
        while (nonce < 0x100000000) {

            val headerBuffer = ByteBuffer.allocate(4 + 32 + 32 + 8 + 8 + 4)
            headerBuffer.put(ByteBuffer.allocate(4).putInt(ver).array()) // version
            headerBuffer.put(Hex.decode(parentHash)) // parentHash
            headerBuffer.put(Hex.decode(merkleRoot)) // trxTrieRoot
            headerBuffer.put(ByteBuffer.allocate(8).putLong(time).array()) // time
            headerBuffer.put(ByteBuffer.allocate(8).putLong(difficulty).array()) // difficulty(current difficulty)
            headerBuffer.put(ByteBuffer.allocate(4).putInt(nonce).array()) // nonce

            val header = headerBuffer.array()
            val hit = Hex.toHexString(sha256(sha256(header)))
            println("$nonce : $hit")

            if (hit < targetStr) {
                println("Got Nonce : $nonce")
                println("Got Hit : $hit")
                break
            }
            nonce += 1
        }
    }

    /**
     * 挖矿难度(Difficulty)运算测试。
     */
    @Test
    fun difficultyTest() {
        val difficulty =
            BigInteger.valueOf(0x0404cbL).multiply(BigInteger.valueOf(2).pow(8 * (0x1b - 3)))
        assertEquals(difficulty.toString(16), "404cb000000000000000000000000000000000000000000000000")
    }

    /**
     * Merkle Root Hash测试。
     */
    @Test
    fun merkleTest() {
        // 初始化Alice账户
        val kp1 = generateKeyPair()
        val alice = Account(kp1.public)

        // 初始化Bob账户
        val kp2 = generateKeyPair()
        val bob = Account(kp2.public)

        // Alice向Bob转账100
        val trx1 =
            Transaction(alice.address, bob.address, BigInteger.valueOf(100), DateTime(), kp1.public)

        // Alice用私钥签名
        trx1.sign(kp1.private)

        // Alice向Bob转账50
        val trx2 =
            Transaction(alice.address, bob.address, BigInteger.valueOf(50), DateTime(), kp1.public)

        // Alice用私钥签名
        trx2.sign(kp1.private)

        val trxTrieRoot = CryptoUtil.merkleRoot(listOf(trx1, trx2))
        println(Hex.toHexString(trxTrieRoot))
    }

    /**
     * 账户状态序列化/反序列化测试。
     */
    @Test
    fun accountStateEncodeTest() {
        val accountState = AccountState(BigInteger.TEN, BigInteger.TEN)

        println(ASN1Dump.dumpAsString(ASN1InputStream(accountState.encode()).readObject()))

        val decoded = CodecUtil.decodeAccountState(accountState.encode())

        assertEquals(accountState.nonce, decoded?.nonce)
        assertEquals(accountState.balance, decoded?.balance)
    }

    @Test
    fun getPublicKeyFromPrivateKeyTest() {
        for (i in 0..100) {
            val kp = generateKeyPair()
            val privateKey = kp.private
            val publicKey = kp.public

            val generatedPublicKey = CryptoUtil.generatePublicKey(privateKey)

            assertArrayEquals(publicKey.encoded, generatedPublicKey?.encoded)

            println("Pass public key from private key test :$i")
        }
    }

}
