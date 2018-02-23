package io.lunarchain.lunarcoin.core

import io.lunarchain.lunarcoin.util.CodecUtil
import io.lunarchain.lunarcoin.util.FastByteComparisons.equal
import io.lunarchain.lunarcoin.util.HashUtil.EMPTY_TRIE_HASH
import java.math.BigInteger

/**
 * 账户状态。
 */
class AccountState(val nonce: BigInteger, val balance: BigInteger) {

    fun encode(): ByteArray {
        return CodecUtil.encodeAccountState(this)
    }

    fun increaseNonce(): AccountState {
        return AccountState(nonce + BigInteger.ONE, balance)
    }


    fun increaseBalance(amount: BigInteger): AccountState {
        return AccountState(nonce, balance + amount)
    }

    //code that represents contract state

    /* A 256-bit hash of the root node of a trie structure
       * that encodes the storage contents of the contract,
       * itself a simple mapping between byte arrays of size 32.
       * The hash is formally denoted σ[a] s .
       *
       * Since I typically wish to refer not to the trie’s root hash
       * but to the underlying set of key/value pairs stored within,
       * I define a convenient equivalence TRIE (σ[a] s ) ≡ σ[a] s .
       * It shall be understood that σ[a] s is not a ‘physical’ member
       * of the account and does not contribute to its later serialisation */
    private var stateRoot: ByteArray? = null

    /* The hash of the EVM code of this contract—this is the code
       * that gets executed should this address receive a message call;
       * it is immutable and thus, unlike all other fields, cannot be changed
       * after construction. All such code fragments are contained in
       * the state database under their corresponding hashes for later
       * retrieval */
    private var codeHash: ByteArray? = null

    constructor(nonce: BigInteger, balance: BigInteger, stateRoot: ByteArray, codeHash: ByteArray): this(nonce, balance) {
        this.stateRoot = if (stateRoot.contentEquals(EMPTY_TRIE_HASH) || equal(stateRoot, EMPTY_TRIE_HASH)) EMPTY_TRIE_HASH else stateRoot
        this.codeHash = codeHash
    }

}
