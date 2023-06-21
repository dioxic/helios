package uk.dioxic.mgenerate.template.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
import uk.dioxic.mgenerate.template.Template
import uk.dioxic.mgenerate.template.Template.Companion.defaultRegistry
import uk.dioxic.mgenerate.template.codecs.TemplateDocumentCodec
import uk.dioxic.mgenerate.template.extensions.asJsonDecoder
import uk.dioxic.mgenerate.template.extensions.asJsonEncoder

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