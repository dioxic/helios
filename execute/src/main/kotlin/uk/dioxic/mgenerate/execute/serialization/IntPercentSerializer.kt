package uk.dioxic.mgenerate.execute.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import uk.dioxic.mgenerate.execute.format.Percent

internal object IntPercentSerializer : KSerializer<Percent> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Percent", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Percent) {
        encoder.encodeString("$value%")
    }

    override fun deserialize(decoder: Decoder): Percent {
        return decoder.decodeString().trimEnd { it == '%' }.toInt()
    }
}

