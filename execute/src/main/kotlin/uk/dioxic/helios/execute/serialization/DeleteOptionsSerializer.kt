package uk.dioxic.helios.execute.serialization

import com.mongodb.client.model.DeleteOptions
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DeleteOptionsSerializer : KSerializer<DeleteOptions> {
    override val descriptor: SerialDescriptor =
        UpdateOptionsSurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): DeleteOptions {
        val surrogate = decoder.decodeSerializableValue(DeleteOptionsSurrogate.serializer())
        return DeleteOptions()
            .comment(surrogate.comment)
            .hint(surrogate.hint)
            .hintString(surrogate.hintString)
    }

    override fun serialize(encoder: Encoder, value: DeleteOptions) {
        encoder.encodeSerializableValue(
            DeleteOptionsSurrogate.serializer(), DeleteOptionsSurrogate(
                comment = value.comment?.asString()?.value,
                hintString = value.hintString,
                hint = value.hint?.toBsonDocument(),
            )
        )
    }
}