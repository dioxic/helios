package uk.dioxic.helios.generate

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import org.bson.Document
import org.bson.UuidRepresentation
import org.bson.codecs.*
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import org.bson.codecs.jsr310.Jsr310CodecProvider
import org.bson.json.JsonMode
import org.bson.json.JsonWriterSettings
import uk.dioxic.helios.generate.codecs.OperatorExecutionCodecProvider
import uk.dioxic.helios.generate.codecs.TemplateDocumentCodecProvider
import uk.dioxic.helios.generate.serialization.TemplateSerializer

@Serializable(TemplateSerializer::class)
open class Template(map: Map<String, *>, val definition: JsonObject? = null) : Document(map) {

    private val defaultJsonWriter = JsonWriterSettings.builder()
        .indent(true)
        .outputMode(JsonMode.RELAXED)
        .build()

    override fun toJson(writerSettings: JsonWriterSettings): String =
        super.toJson(writerSettings, defaultCodec)

    override fun toJson(): String =
        super.toJson(defaultJsonWriter, defaultCodec)

    companion object {
        val defaultRegistry: CodecRegistry = CodecRegistries.fromProviders(
            listOf(
                ValueCodecProvider(),
                Jsr310CodecProvider(),
                TemplateCodecProvider(),
                CollectionCodecProvider(OperatorTransformer),
                IterableCodecProvider(OperatorTransformer),
                OperatorExecutionCodecProvider(),
                BsonValueCodecProvider(),
                DocumentCodecProvider(OperatorTransformer),
                MapCodecProvider()
            )
        )
        private val defaultCodec = CodecRegistries.withUuidRepresentation(
            defaultRegistry,
            UuidRepresentation.STANDARD
        )[Document::class.java]

        val EMPTY = Template(emptyMap<String, Any>(), JsonObject(mapOf()))

    }
}

