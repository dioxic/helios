package uk.dioxic.mgenerate.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
import uk.dioxic.mgenerate.Template
import uk.dioxic.mgenerate.Template.Companion.defaultRegistry
import uk.dioxic.mgenerate.codecs.TemplateDocumentCodec
import uk.dioxic.mgenerate.extensions.asJsonDecoder
import uk.dioxic.mgenerate.extensions.asJsonEncoder

object TemplateSerializer : KSerializer<Template> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("TemplateSerializer", PrimitiveKind.STRING)

    private val templateCodec = TemplateDocumentCodec(defaultRegistry)

    override fun deserialize(decoder: Decoder): Template =
        when (val jsonElement = decoder.asJsonDecoder().decodeJsonElement()) {
            is JsonObject -> templateCodec.decode(jsonElement)
            else -> error("expecting as JsonObject but ${jsonElement::class} was found")
        }

    override fun serialize(encoder: Encoder, value: Template) {
        require(value.definition != null) {
            "Template definition not set"
        }
        encoder.asJsonEncoder().encodeJsonElement(value.definition)
    }
}