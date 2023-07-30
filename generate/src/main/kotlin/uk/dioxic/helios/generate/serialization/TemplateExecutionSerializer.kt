package uk.dioxic.helios.generate.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.bson.asBsonEncoder
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bson.codecs.EncoderContext
import uk.dioxic.helios.generate.Template
import uk.dioxic.helios.generate.codecs.DocumentCodec

@Deprecated("Probably not required")
object TemplateExecutionSerializer : KSerializer<Template> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("TemplateSerializer", PrimitiveKind.STRING)

    private val documentCodec = DocumentCodec()

    override fun deserialize(decoder: Decoder): Template {
        throw UnsupportedOperationException("cannot deserialize a hydrated template")
    }

    override fun serialize(encoder: Encoder, value: Template) {
        documentCodec.encode(
            writer = encoder.asBsonEncoder().writer(),
            document = value,
            encoderContext = EncoderContext.builder().build()
        )
    }
}