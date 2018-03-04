package io.lunarchain.lunarcoin.network.message

import io.lunarchain.lunarcoin.core.BlockHeader
import io.lunarchain.lunarcoin.util.CodecUtil
import org.spongycastle.asn1.ASN1EncodableVector
import org.spongycastle.asn1.ASN1InputStream
import org.spongycastle.asn1.ASN1Sequence
import org.spongycastle.asn1.DERSequence

class BlockHeadersMessage(val headers: List<BlockHeader>) : Message {
    override fun code(): Byte = MessageCodes.BLOCK_HEADERS.code

    override fun encode(): ByteArray {

        val v = ASN1EncodableVector()

        headers.forEach { v.add(CodecUtil.encodeBlockHeaderToAsn1(it)) }

        return DERSequence(v).encoded
    }

    companion object {
        fun decode(bytes: ByteArray): BlockHeadersMessage? {
            val v = ASN1InputStream(bytes).readObject()

            if (v != null) {
                val seq = ASN1Sequence.getInstance(v)

                val headers = mutableListOf<BlockHeader>()
                for (element in seq.objects) {
                    val der = DERSequence.getInstance(element)
                    val header = CodecUtil.decodeBlockHeader(der.encoded)
                    if (header != null) {
                        headers.add(header)
                    }
                }

                return BlockHeadersMessage(headers)
            }

            return null
        }
    }
}
