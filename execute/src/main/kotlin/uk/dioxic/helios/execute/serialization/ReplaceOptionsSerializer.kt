package uk.dioxic.helios.execute.serialization

import com.mongodb.client.model.ReplaceOptions
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ReplaceOptionsSerializer : KSerializer<ReplaceOptions> {
    override val descriptor: SerialDescriptor =
        UpdateOptionsSurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): ReplaceOptions {
        val surrogate = decoder.decodeSerializableValue(ReplaceOptionsSurrogate.serializer())
        return ReplaceOptions()
            .upsert(surrogate.upsert)
            .comment(surrogate.comment)
            .hint(surrogate.hint)
            .hintString(surrogate.hintString)
            .bypassDocumentValidation(surrogate.bypassDocumentValidation)

    }

    override fun serialize(encoder: Encoder, value: ReplaceOptions) {
        encoder.encodeSerializableValue(
            ReplaceOptionsSurrogate.serializer(), ReplaceOptionsSurrogate(
                upsert = value.isUpsert,
                comment = value.comment?.asString()?.value,
                hintString = value.hintString,
                hint = value.hint?.toBsonDocument(),
                bypassDocumentValidation = value.bypassDocumentValidation,
            )
        )
    }
}