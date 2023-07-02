package uk.dioxic.helios.generate.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import uk.dioxic.helios.generate.Template
import uk.dioxic.helios.generate.codecs.TemplateCodec
import uk.dioxic.helios.generate.extensions.asJsonDecoder
import uk.dioxic.helios.generate.extensions.asJsonEncoder

object TemplateSerializer : KSerializer<Template> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("TemplateSerializer", PrimitiveKind.STRING)

    private val templateCodec = TemplateCodec()

    override fun deserialize(decoder: Decoder): Template =
        templateCodec.decode(decoder.asJsonDecoder().decodeJsonElement())

    override fun serialize(encoder: Encoder, value: Template) {
        require(value.definition != null) {
            "Template definition not set"
        }
        encoder.asJsonEncoder().encodeJsonElement(value.definition)
    }
}