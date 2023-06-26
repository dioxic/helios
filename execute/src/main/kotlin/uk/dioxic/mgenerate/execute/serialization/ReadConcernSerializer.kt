package uk.dioxic.mgenerate.execute.serialization

import com.mongodb.ReadConcern
import com.mongodb.ReadConcernLevel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ReadConcernSerializer : KSerializer<ReadConcern> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ReadConcern", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ReadConcern =
        ReadConcern(ReadConcernLevel.fromString(decoder.decodeString()))


    override fun serialize(encoder: Encoder, value: ReadConcern) {
        encoder.encodeString(value.level!!.value)
    }

}