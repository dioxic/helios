package uk.dioxic.helios.execute.serialization

import com.mongodb.client.model.InsertOneOptions
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object InsertOneOptionsSerializer : KSerializer<InsertOneOptions> {
    override val descriptor: SerialDescriptor =
        InsertOneOptionsSurrogate.serializer().descriptor


    override fun deserialize(decoder: Decoder): InsertOneOptions {
        val surrogate = decoder.decodeSerializableValue(UpdateOptionsSurrogate.serializer())
        return InsertOneOptions()
            .comment(surrogate.comment)
            .bypassDocumentValidation(surrogate.bypassDocumentValidation)

    }

    override fun serialize(encoder: Encoder, value: InsertOneOptions) {
        encoder.encodeSerializableValue(
            InsertOneOptionsSurrogate.serializer(), InsertOneOptionsSurrogate(
                comment = value.comment?.asString()?.value,
                bypassDocumentValidation = value.bypassDocumentValidation,
            )
        )
    }
}