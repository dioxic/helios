package uk.dioxic.helios.execute.serialization

import com.mongodb.client.model.BulkWriteOptions
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object BulkWriteOptionsSerializer : KSerializer<BulkWriteOptions> {
    override val descriptor: SerialDescriptor =
        UpdateOptionsSurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): BulkWriteOptions {
        val surrogate = decoder.decodeSerializableValue(BulkWriteOptionsSurrogate.serializer())
        return BulkWriteOptions()
            .comment(surrogate.comment)
            .ordered(surrogate.ordered)
            .bypassDocumentValidation(surrogate.bypassDocumentValidation)

    }

    override fun serialize(encoder: Encoder, value: BulkWriteOptions) {
        encoder.encodeSerializableValue(
            BulkWriteOptionsSurrogate.serializer(), BulkWriteOptionsSurrogate(
                comment = value.comment?.asString()?.value,
                ordered = value.isOrdered,
                bypassDocumentValidation = value.bypassDocumentValidation,
            )
        )
    }
}