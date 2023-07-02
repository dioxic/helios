package uk.dioxic.helios.generate.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bson.Document
import org.bson.codecs.DecoderContext
import uk.dioxic.helios.generate.Template
import uk.dioxic.helios.generate.codecs.TemplateCodec
import uk.dioxic.helios.generate.extensions.asBsonDecoder
import uk.dioxic.helios.generate.extensions.asBsonEncoder

object BsonTemplateSerializer : KSerializer<Template> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BsonTemplateSerializer", PrimitiveKind.STRING)

    private val templateCodec = TemplateCodec()

    override fun deserialize(decoder: Decoder): Template =
        templateCodec.decode(decoder.asBsonDecoder().reader(), DecoderContext.builder().build())

    override fun serialize(encoder: Encoder, value: Template) {
        require(value.definition != null) {
            "Template definition not set"
        }
        encoder.asBsonEncoder().encodeBsonValue(Document(value.definition).toBsonDocument())
    }
}