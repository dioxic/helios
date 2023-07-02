package uk.dioxic.helios.generate

import kotlinx.serialization.Serializable
import org.bson.Document
import org.bson.UuidRepresentation
import org.bson.codecs.Encoder
import org.bson.codecs.EncoderContext
import org.bson.json.JsonMode
import org.bson.json.JsonWriter
import org.bson.json.JsonWriterSettings
import uk.dioxic.helios.generate.codecs.TemplateCodec
import uk.dioxic.helios.generate.serialization.BsonTemplateSerializer
import java.io.StringWriter

@Serializable(BsonTemplateSerializer::class)
class Template(map: Map<String, *>, val definition: Map<String, *>? = null) : Document(map) {

    private val defaultJsonWriter = JsonWriterSettings.builder()
        .indent(true)
        .outputMode(JsonMode.RELAXED)
        .build()

    override fun toJson(writerSettings: JsonWriterSettings?, encoder: Encoder<Document>): String =
        throw UnsupportedOperationException()

    override fun toJson(writerSettings: JsonWriterSettings): String {
        val writer = JsonWriter(StringWriter(), writerSettings)
        defaultCodec.encode(writer, this, EncoderContext.builder().build())
        return writer.writer.toString()
    }

    override fun toJson(): String =
        this.toJson(defaultJsonWriter)

    companion object {
        val defaultCodec = TemplateCodec(uuidRepresentation = UuidRepresentation.STANDARD)

        val EMPTY = Template(emptyMap<String, Any>())

    }
}

