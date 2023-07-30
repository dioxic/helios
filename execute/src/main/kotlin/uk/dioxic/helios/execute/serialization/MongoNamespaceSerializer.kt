package uk.dioxic.helios.execute.serialization

import com.mongodb.MongoNamespace
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object MongoNamespaceSerializer : KSerializer<MongoNamespace> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MongoNamespace", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): MongoNamespace {
        return MongoNamespace(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: MongoNamespace) {
        encoder.encodeString(value.fullName)
    }
}