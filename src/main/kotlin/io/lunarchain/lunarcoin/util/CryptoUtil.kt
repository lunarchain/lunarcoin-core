package io.lunarchain.lunarcoin.util

import io.lunarchain.lunarcoin.core.Block
import io.lunarchain.lunarcoin.core.Transaction
import io.lunarchain.lunarcoin.trie.Trie
import io.lunarchain.lunarcoin.util.BIUtil.isLessThan
import org.spongycastle.asn1.sec.SECNamedCurves
import org.spongycastle.asn1.x9.X9IntegerConverter
import org.spongycastle.crypto.params.ECDomainParameters
import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.spongycastle.jce.ECNamedCurveTable
import org.spongycastle.jce.provider.BouncyCastleProvider
import org.spongycastle.jce.spec.ECPublicKeySpec
import org.spongycastle.math.ec.ECAlgorithms
import org.spongycastle.math.ec.ECCurve
import org.spongycastle.math.ec.ECPoint
import java.math.BigInteger
import java.security.*
import java.security.Security.insertProviderAt
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.EncryptedPrivateKeyInfo
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.PBEParameterSpec


/**
 * 密码学工具类。
 */
class CryptoUtil {

    companion object {
        init {
            insertProviderAt(BouncyCastleProvider(), 1)
        }

        val params = SECNamedCurves.getByName("secp256k1")
        val CURVE: ECDomainParameters = ECDomainParameters(params.curve, params.g, params.n, params.h);

        /**
         * 根据公钥(public key)推算出账户地址，使用以太坊的算法，先KECCAK-256计算哈希值(32位)，取后20位作为账户地址。
         * 比特币地址算法：http://www.infoq.com/cn/articles/bitcoin-and-block-chain-part03
         * 以太坊地址算法：http://ethereum.stackexchange.com/questions/3542/how-are-ethereum-addresses-generated
         */
        fun generateAddress(publicKey: PublicKey): ByteArray {
            val digest = MessageDigest.getInstance("KECCAK-256", "SC")
            digest.update(publicKey.encoded)
            val hash = digest.digest()

            return hash.drop(12).toByteArray()
        }

        /**
         * 生成公私钥对，使用以太坊的ECDSA算法(secp256k1)。
         */
        fun generateKeyPair(): KeyPair {
            val gen = KeyPairGenerator.getInstance("EC", "SC")
            gen.initialize(ECGenParameterSpec("secp256k1"), SecureRandom())
            val keyPair = gen.generateKeyPair()
            return keyPair
        }

        /**
         * 发送方用私钥对交易Transaction进行签名。
         */
        fun signTransaction(trx:Transaction, privateKey: PrivateKey): ByteArray {
            val signer = Signature.getInstance("SHA256withECDSA")
            signer.initSign(privateKey)
            val msgToSign = CodecUtil.encodeTransactionWithoutSignatureToAsn1(trx).encoded
            signer.update(msgToSign)
            return signer.sign()
        }

        /**
         * 验证交易Transaction签名的有效性。
         */
        fun verifyTransactionSignature(trx: Transaction, signature: ByteArray): Boolean {
            val signer = Signature.getInstance("SHA256withECDSA")
            signer.initVerify(trx.publicKey)

            signer.update(CodecUtil.encodeTransactionWithoutSignatureToAsn1(trx).encoded)
            return signer.verify(signature)
        }

        /**
         * 运算区块的哈希值。
         */
        fun hashBlock(block: Block): ByteArray {
            val digest = MessageDigest.getInstance("KECCAK-256", "SC")
            digest.update(block.encode())
            return digest.digest()
        }


        /**
         * 计算Merkle Root Hash
         */
        fun merkleRoot(transactions: List<Transaction>): ByteArray {
            val trxTrie = Trie<Transaction>()
            for (i in 0 until transactions.size) {
                trxTrie.put(i.toString(), transactions[i])
            }
            return trxTrie.root?.hash() ?: ByteArray(0)
        }

        /**
         * SHA-256
         */
        fun sha256(msg: ByteArray): ByteArray {
            val digest = MessageDigest.getInstance("SHA-256", "SC")
            digest.update(msg)
            val hash = digest.digest()

            return hash
        }

        /**
         * SHA3
         */
        fun sha3(msg: ByteArray): ByteArray {
            return sha256((msg))
        }

        fun deserializePrivateKey(bytes: ByteArray): PrivateKey {
            val kf = KeyFactory.getInstance("EC", "SC")
            return kf.generatePrivate(PKCS8EncodedKeySpec(bytes))
        }

        fun deserializePublicKey(bytes: ByteArray): PublicKey {
            val kf = KeyFactory.getInstance("EC", "SC")
            return kf.generatePublic(X509EncodedKeySpec(bytes))
        }

        /**
         * 从PrivateKey计算出PublicKey，参考了以太坊的代码和http://stackoverflow.com/questions/26159149/how-can-i-default-a-publickey-object-from-ec-public-key-bytes
         */
        fun generatePublicKey(privateKey: PrivateKey): PublicKey? {
            val spec = ECNamedCurveTable.getParameterSpec("secp256k1")
            val kf = KeyFactory.getInstance("EC", "SC")

            val curve = ECDomainParameters(spec.curve, spec.g, spec.n, spec.h)

            if (privateKey is BCECPrivateKey) {
                val d = privateKey.d
                val point = curve.g.multiply(d)
                val pubKeySpec = ECPublicKeySpec(point, spec)
                val publicKey = kf.generatePublic(pubKeySpec) as ECPublicKey
                return publicKey
            } else {
                return null
            }
        }

        /**
         * Encrypt private key in PKCS8 form with the password.
         */
        fun encryptPrivateKey(privateKey: PrivateKey, password: String): ByteArray {
            // We must use a PasswordBasedEncryption algorithm in order to encrypt the private key, you may use any common algorithm supported by openssl, you can check them in the openssl documentation http://www.openssl.org/docs/apps/pkcs8.html
            val MYPBEALG = "PBEWithSHA1AndDESede"

            val count = 20// hash iteration count
            val random = SecureRandom()
            val salt = ByteArray(8)
            random.nextBytes(salt)

            // Create PBE parameter set
            val pbeParamSpec = PBEParameterSpec(salt, count)
            val pbeKeySpec = PBEKeySpec(password.toCharArray())
            val keyFac = SecretKeyFactory.getInstance(MYPBEALG)
            val pbeKey = keyFac.generateSecret(pbeKeySpec)

            val pbeCipher = Cipher.getInstance(MYPBEALG)

            // Initialize PBE Cipher with key and parameters
            pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec)

            // Encrypt the encoded Private Key with the PBE key
            val ciphertext = pbeCipher.doFinal(privateKey.encoded)

            // Now construct  PKCS #8 EncryptedPrivateKeyInfo object
            val algparms = AlgorithmParameters.getInstance(MYPBEALG)
            algparms.init(pbeParamSpec)
            val encinfo = EncryptedPrivateKeyInfo(algparms, ciphertext)

            // and here we have it! a DER encoded PKCS#8 encrypted key!
            val encryptedPkcs8 = encinfo.getEncoded()

            return encryptedPkcs8
        }

        /**
         * Decrypt the encrypted PKCS8 private key with the password.
         */
        fun decryptPrivateKey(encryptedPkcs8: ByteArray, password: String): PrivateKey {
            val encryptPKInfo = EncryptedPrivateKeyInfo(encryptedPkcs8)

            val cipher = Cipher.getInstance(encryptPKInfo.algName)
            val pbeKeySpec = PBEKeySpec(password.toCharArray())
            val secFac = SecretKeyFactory.getInstance(encryptPKInfo.algName)
            val pbeKey = secFac.generateSecret(pbeKeySpec)
            val algParams = encryptPKInfo.algParameters
            cipher.init(Cipher.DECRYPT_MODE, pbeKey, algParams)
            val pkcs8KeySpec = encryptPKInfo.getKeySpec(cipher)
            val kf = KeyFactory.getInstance("EC", "SC")
            return kf.generatePrivate(pkcs8KeySpec)
        }

        fun hashTransaction(trx: Transaction): ByteArray {
            val digest = MessageDigest.getInstance("KECCAK-256", "SC")
            digest.update(trx.encode())
            return digest.digest()
        }

        class ECDSASignature() {

            private val SECP256K1N = BigInteger("fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141", 16)
            /**
             * The two components of the signature.
             */
            var r: BigInteger? = null
            var s: BigInteger? = null
            var v: Byte = 0

            constructor(r: BigInteger, s: BigInteger): this() {
                this.r = r
                this.s = s
            }


            /**
             * t
             * @param r
             * @param s
             * @return -
             */
            private fun fromComponents(r: ByteArray, s: ByteArray): ECDSASignature {
                return ECDSASignature(BigInteger(1, r), BigInteger(1, s))
            }

            /**
             *
             * @param r -
             * @param s -
             * @param v -
             * @return -
             */
            fun fromComponents(r: ByteArray, s: ByteArray, v: Byte): ECDSASignature {
                val signature = fromComponents(r, s)
                signature.v = v
                return signature
            }

            fun validateComponents(): Boolean {
                return validateComponents(r, s, v)
            }

            fun validateComponents(r: BigInteger?, s: BigInteger?, v: Byte): Boolean {
                if(r == null || s == null) return false
                if (v.toInt() != 27 && v.toInt() != 28) return false

                if (isLessThan(r, BigInteger.ONE)) return false
                if (isLessThan(s, BigInteger.ONE)) return false

                if (!isLessThan(r, SECP256K1N)) return false
                return if (!isLessThan(s, SECP256K1N)) false else true

            }

        }


        /**
         * Decompress a compressed public key (x co-ord and low-bit of y-coord).
         *
         * @param xBN -
         * @param yBit -
         * @return -
         */
        private fun decompressKey(xBN: BigInteger, yBit: Boolean): ECPoint {
            val x9 = X9IntegerConverter()
            val compEnc = x9.integerToBytes(xBN, 1 + x9.getByteLength(CURVE.getCurve()))
            compEnc[0] = (if (yBit) 0x03 else 0x02).toByte()
            return CURVE.getCurve().decodePoint(compEnc)
        }

        fun recoverPubBytesFromSignature(recId: Int, sig: ECDSASignature, messageHash: ByteArray?): ByteArray? {
            check(recId >= 0, "recId must be positive")
            check(sig.r!!.signum() >= 0, "r must be positive")
            check(sig.s!!.signum() >= 0, "s must be positive")
            check(messageHash != null, "messageHash must not be null")
            // 1.0 For j from 0 to h   (h == recId here and the loop is outside this function)
            //   1.1 Let x = r + jn
            val n = CURVE.getN()  // Curve order.
            val i = BigInteger.valueOf(recId.toLong() / 2)
            val x = sig.r!!.add(i.multiply(n))
            //   1.2. Convert the integer x to an octet string X of length mlen using the conversion routine
            //        specified in Section 2.3.7, where mlen = ⌈(log2 p)/8⌉ or mlen = ⌈m/8⌉.
            //   1.3. Convert the octet string (16 set binary digits)||X to an elliptic curve point R using the
            //        conversion routine specified in Section 2.3.4. If this conversion routine outputs “invalid”, then
            //        do another iteration of Step 1.
            //
            // More concisely, what these points mean is to use X as a compressed public key.
            val curve = CURVE.getCurve() as ECCurve.Fp
            val prime = curve.q  // Bouncy Castle is not consistent about the letter it uses for the prime.
            if (x.compareTo(prime) >= 0) {
                // Cannot have point co-ordinates larger than this as everything takes place modulo Q.
                return null
            }
            // Compressed keys require you to know an extra bit of data about the y-coord as there are two possibilities.
            // So it's encoded in the recId.
            val R = decompressKey(x, recId and 1 == 1)
            //   1.4. If nR != point at infinity, then do another iteration of Step 1 (callers responsibility).
            if (!R.multiply(n).isInfinity())
                return null
            //   1.5. Compute e from M using Steps 2 and 3 of ECDSA signature verification.
            val e = BigInteger(1, messageHash!!)
            //   1.6. For k from 1 to 2 do the following.   (loop is outside this function via iterating recId)
            //   1.6.1. Compute a candidate public key as:
            //               Q = mi(r) * (sR - eG)
            //
            // Where mi(x) is the modular multiplicative inverse. We transform this into the following:
            //               Q = (mi(r) * s ** R) + (mi(r) * -e ** G)
            // Where -e is the modular additive inverse of e, that is z such that z + e = 0 (mod n). In the above equation
            // ** is point multiplication and + is point addition (the EC group operator).
            //
            // We can find the additive inverse by subtracting e from zero then taking the mod. For example the additive
            // inverse of 3 modulo 11 is 8 because 3 + 8 mod 11 = 0, and -3 mod 11 = 8.
            val eInv = BigInteger.ZERO.subtract(e).mod(n)
            val rInv = sig.r!!.modInverse(n)
            val srInv = rInv.multiply(sig.s).mod(n)
            val eInvrInv = rInv.multiply(eInv).mod(n)
            val q = ECAlgorithms.sumOfTwoMultiplies(CURVE.getG(), eInvrInv, R, srInv) as ECPoint.Fp
            return q.getEncoded(/* compressed */false)
        }


        private fun check(test: Boolean, message: String) {
            if (!test) throw IllegalArgumentException(message)
        }

        @Throws(SignatureException::class)
        fun signatureToKeyBytes(messageHash: ByteArray, sig: ECDSASignature): ByteArray {
            check(messageHash.size == 32, "messageHash argument has length " + messageHash.size)
            var header = sig.v.toInt()
            // The header byte: 0x1B = first key with even y, 0x1C = first key with odd y,
            //                  0x1D = second key with even y, 0x1E = second key with odd y
            if (header < 27 || header > 34)
                throw SignatureException("Header byte out of range: " + header)
            if (header >= 31) {
                header -= 4
            }
            val recId = header - 27
            return this.recoverPubBytesFromSignature(recId, sig, messageHash)
                    ?: throw SignatureException("Could not recover public key from signature")
        }


        /**
         * Compute an address from an encoded public key.
         *
         * @param pubBytes an encoded (uncompressed) public key
         * @return 20-byte address
         */
        fun computeAddress(pubBytes: ByteArray): ByteArray {
            return HashUtil.sha3omit12(
                Arrays.copyOfRange(pubBytes, 1, pubBytes.size)
            )
        }

        /**
         * Compute the address of the key that signed the given signature.
         *
         * @param messageHash 32-byte hash of message
         * @param sig -
         * @return 20-byte address
         */
        @Throws(SignatureException::class)
        fun signatureToAddress(messageHash: ByteArray, sig: ECDSASignature): ByteArray {
            return computeAddress(signatureToKeyBytes(messageHash, sig))
        }


    }

}
