package uk.dioxic.mgenerate.execute.serialization

import com.mongodb.ReadPreference
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ReadPreferenceSerializer : KSerializer<ReadPreference> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ReadPreference", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ReadPreference =
        ReadPreference.valueOf(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: ReadPreference) {
        encoder.encodeString(value.name)
    }

}