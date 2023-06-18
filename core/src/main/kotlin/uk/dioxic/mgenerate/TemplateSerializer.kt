package uk.dioxic.mgenerate

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.bson.codecs.DecoderContext
import org.bson.json.JsonReader
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
            is JsonObject -> {
                val bsonReader = JsonReader(Json.encodeToString(jsonElement))
                templateCodec.decode(bsonReader, DecoderContext.builder().build(), jsonElement)
            }
            else -> error("expecting as JsonObject but ${jsonElement::class} was found")
        }

    override fun serialize(encoder: Encoder, value: Template) {
        require(value.definition != null) {
            "Template definition not set"
        }
        encoder.asJsonEncoder().encodeJsonElement(value.definition)
    }
}