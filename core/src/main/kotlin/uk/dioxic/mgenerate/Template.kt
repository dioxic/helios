package uk.dioxic.mgenerate

import org.bson.Document
import org.bson.UuidRepresentation
import org.bson.codecs.*
import org.bson.codecs.configuration.CodecRegistries
import org.bson.json.JsonReader
import org.bson.json.JsonWriterSettings
import uk.dioxic.mgenerate.codecs.OperatorExecutionCodecProvider
import uk.dioxic.mgenerate.codecs.TemplateDocumentCodec

class Template(map: Map<String, *>) : Document(map) {

    override fun toJson(writerSettings: JsonWriterSettings): String =
        super.toJson(writerSettings, defaultCodec)

    override fun toJson(): String =
        super.toJson(defaultCodec)

    companion object {
        private val defaultRegistry = CodecRegistries.fromProviders(
            listOf(
                ValueCodecProvider(),
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