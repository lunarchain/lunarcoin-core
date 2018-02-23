package lunar.vm

class GasCost {
    /* backwards compatibility, remove eventually */
    private val STEP = 1
    private val SSTORE = 300
    /* backwards compatibility, remove eventually */

    private val ZEROSTEP = 0
    private val QUICKSTEP = 2
    private val FASTESTSTEP = 3
    private val FASTSTEP = 5
    private val MIDSTEP = 8
    private val SLOWSTEP = 10
    private val EXTSTEP = 20

    private val GENESISGASLIMIT = 1000000
    private val MINGASLIMIT = 125000

    private val BALANCE = 20
    private val SHA3 = 30
    private val SHA3_WORD = 6
    private val SLOAD = 50
    private val STOP = 0
    private val SUICIDE = 0
    private val CLEAR_SSTORE = 5000
    private val SET_SSTORE = 20000
    private val RESET_SSTORE = 5000
    private val REFUND_SSTORE = 15000
    private val CREATE = 32000

    private val JUMPDEST = 1
    private val CREATE_DATA_BYTE = 5
    private val CALL = 40
    private val STIPEND_CALL = 2300
    private val VT_CALL = 9000  //value transfer call
    private val NEW_ACCT_CALL = 25000  //new account call
    private val MEMORY = 3
    private val SUICIDE_REFUND = 24000
    private val QUAD_COEFF_DIV = 512
    private val CREATE_DATA = 200
    private val TX_NO_ZERO_DATA = 68
    private val TX_ZERO_DATA = 4
    private val TRANSACTION = 21000
    private val TRANSACTION_CREATE_CONTRACT = 53000
    private val LOG_GAS = 375
    private val LOG_DATA_GAS = 8
    private val LOG_TOPIC_GAS = 375
    private val COPY_GAS = 3
    private val EXP_GAS = 10
    private val EXP_BYTE_GAS = 10
    private val IDENTITY = 15
    private val IDENTITY_WORD = 3
    private val RIPEMD160 = 600
    private val RIPEMD160_WORD = 120
    private val SHA256 = 60
    private val SHA256_WORD = 12
    private val EC_RECOVER = 3000
    private val EXT_CODE_SIZE = 20
    private val EXT_CODE_COPY = 20
    private val NEW_ACCT_SUICIDE = 0

    fun getSTEP(): Int {
        return STEP
    }

    fun getSSTORE(): Int {
        return SSTORE
    }

    fun getZEROSTEP(): Int {
        return ZEROSTEP
    }

    fun getQUICKSTEP(): Int {
        return QUICKSTEP
    }

    fun getFASTESTSTEP(): Int {
        return FASTESTSTEP
    }

    fun getFASTSTEP(): Int {
        return FASTSTEP
    }

    fun getMIDSTEP(): Int {
        return MIDSTEP
    }

    fun getSLOWSTEP(): Int {
        return SLOWSTEP
    }

    fun getEXTSTEP(): Int {
        return EXTSTEP
    }

    fun getGENESISGASLIMIT(): Int {
        return GENESISGASLIMIT
    }

    fun getMINGASLIMIT(): Int {
        return MINGASLIMIT
    }

    fun getBALANCE(): Int {
        return BALANCE
    }

    fun getSHA3(): Int {
        return SHA3
    }

    fun getSHA3_WORD(): Int {
        return SHA3_WORD
    }

    fun getSLOAD(): Int {
        return SLOAD
    }

    fun getSTOP(): Int {
        return STOP
    }

    fun getSUICIDE(): Int {
        return SUICIDE
    }

    fun getCLEAR_SSTORE(): Int {
        return CLEAR_SSTORE
    }

    fun getSET_SSTORE(): Int {
        return SET_SSTORE
    }

    fun getRESET_SSTORE(): Int {
        return RESET_SSTORE
    }

    fun getREFUND_SSTORE(): Int {
        return REFUND_SSTORE
    }

    fun getCREATE(): Int {
        return CREATE
    }

    fun getJUMPDEST(): Int {
        return JUMPDEST
    }

    fun getCREATE_DATA_BYTE(): Int {
        return CREATE_DATA_BYTE
    }

    fun getCALL(): Int {
        return CALL
    }

    fun getSTIPEND_CALL(): Int {
        return STIPEND_CALL
    }

    fun getVT_CALL(): Int {
        return VT_CALL
    }

    fun getNEW_ACCT_CALL(): Int {
        return NEW_ACCT_CALL
    }

    fun getNEW_ACCT_SUICIDE(): Int {
        return NEW_ACCT_SUICIDE
    }

    fun getMEMORY(): Int {
        return MEMORY
    }

    fun getSUICIDE_REFUND(): Int {
        return SUICIDE_REFUND
    }

    fun getQUAD_COEFF_DIV(): Int {
        return QUAD_COEFF_DIV
    }

    fun getCREATE_DATA(): Int {
        return CREATE_DATA
    }

    fun getTX_NO_ZERO_DATA(): Int {
        return TX_NO_ZERO_DATA
    }

    fun getTX_ZERO_DATA(): Int {
        return TX_ZERO_DATA
    }

    fun getTRANSACTION(): Int {
        return TRANSACTION
    }

    fun getTRANSACTION_CREATE_CONTRACT(): Int {
        return TRANSACTION_CREATE_CONTRACT
    }

    fun getLOG_GAS(): Int {
        return LOG_GAS
    }

    fun getLOG_DATA_GAS(): Int {
        return LOG_DATA_GAS
    }

    fun getLOG_TOPIC_GAS(): Int {
        return LOG_TOPIC_GAS
    }

    fun getCOPY_GAS(): Int {
        return COPY_GAS
    }

    fun getEXP_GAS(): Int {
        return EXP_GAS
    }

    fun getEXP_BYTE_GAS(): Int {
        return EXP_BYTE_GAS
    }

    fun getIDENTITY(): Int {
        return IDENTITY
    }

    fun getIDENTITY_WORD(): Int {
        return IDENTITY_WORD
    }

    fun getRIPEMD160(): Int {
        return RIPEMD160
    }

    fun getRIPEMD160_WORD(): Int {
        return RIPEMD160_WORD
    }

    fun getSHA256(): Int {
        return SHA256
    }

    fun getSHA256_WORD(): Int {
        return SHA256_WORD
    }

    fun getEC_RECOVER(): Int {
        return EC_RECOVER
    }

    fun getEXT_CODE_SIZE(): Int {
        return EXT_CODE_SIZE
    }

    fun getEXT_CODE_COPY(): Int {
        return EXT_CODE_COPY
    }
}