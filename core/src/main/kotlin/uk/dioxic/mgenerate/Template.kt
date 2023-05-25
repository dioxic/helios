package uk.dioxic.mgenerate

import org.bson.Document
import org.bson.UuidRepresentation
import org.bson.codecs.*
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.jsr310.Jsr310CodecProvider
import org.bson.json.JsonMode
import org.bson.json.JsonReader
import org.bson.json.JsonWriterSettings
import uk.dioxic.mgenerate.codecs.OperatorExecutionCodecProvider
import uk.dioxic.mgenerate.codecs.TemplateDocumentCodec

class Template(map: Map<String, *>) : Document(map) {

    private val defaultJsonWriter = JsonWriterSettings.builder()
        .indent(true)
        .outputMode(JsonMode.RELAXED)
        .build()

    override fun toJson(writerSettings: JsonWriterSettings): String =
        super.toJson(writerSettings, defaultCodec)

    override fun toJson(): String =
        super.toJson(defaultJsonWriter, defaultCodec)

    companion object {
        private val defaultRegistry = CodecRegistries.fromProviders(
            listOf(
                ValueCodecProvider(), Jsr310CodecProvider(),
                CollectionCodecProvider(), IterableCodecProvider(), OperatorExecutionCodecProvider(),
                BsonValueCodecProvider(), DocumentCodecProvider(OperatorTransformer()), MapCodecProvider()
            )
        )
        private val defaultCodec = CodecRegistries.withUuidRepresentation(
            defaultRegistry,
            UuidRepresentation.STANDARD
        )[Document::class.java]

        fun parse(json: String) =
            parse(json, TemplateDocumentCodec(defaultRegistry))

        fun parse(json: String, decoder: Decoder<Template>): Template {
            val bsonReader = JsonReader(json)
            return decoder.decode(bsonReader, DecoderContext.builder().build())
        }
    }
}