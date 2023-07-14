package uk.dioxic.helios.execute.serialization

import com.mongodb.client.model.UpdateOptions
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object UpdateOptionsSerializer : KSerializer<UpdateOptions> {
    override val descriptor: SerialDescriptor =
        UpdateOptionsSurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): UpdateOptions {
        val surrogate = decoder.decodeSerializableValue(UpdateOptionsSurrogate.serializer())
        return UpdateOptions()
            .upsert(surrogate.upsert)
            .comment(surrogate.comment)
            .arrayFilters(surrogate.arrayFilters)
            .hint(surrogate.hint)
            .hintString(surrogate.hintString)
            .bypassDocumentValidation(surrogate.bypassDocumentValidation)

    }

    override fun serialize(encoder: Encoder, value: UpdateOptions) {
        encoder.encodeSerializableValue(
            UpdateOptionsSurrogate.serializer(), UpdateOptionsSurrogate(
                upsert = value.isUpsert,
                comment = value.comment?.asString()?.value,
                arrayFilters = value.arrayFilters?.map { it.toBsonDocument() },
                hintString = value.hintString,
                hint = value.hint?.toBsonDocument(),
                bypassDocumentValidation = value.bypassDocumentValidation,
            )
        )
    }
}