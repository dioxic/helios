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
import uk.dioxic.helios.generate.operators.Operator
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

    fun hydrate() = hydrateMap(this)

    companion object {
        val defaultRegistry: CodecRegistry = CodecRegistries.fromProviders(
            listOf(
                ValueCodecProvider(), Jsr310CodecProvider(), TemplateDocumentCodecProvider(),
                CollectionCodecProvider(), IterableCodecProvider(), OperatorExecutionCodecProvider(),
                BsonValueCodecProvider(), DocumentCodecProvider(OperatorTransformer()), MapCodecProvider()
            )
        )
        private val defaultCodec = CodecRegistries.withUuidRepresentation(
            defaultRegistry,
            UuidRepresentation.STANDARD
        )[Document::class.java]

        val EMPTY = Template(emptyMap<String, Any>(), JsonObject(mapOf()))

    }
}

@Suppress("UNCHECKED_CAST")
private fun hydrateMap(map: Map<String, Any?>): Map<String, Any?> =
    map.mapValues { (_, v) ->
        when (v) {
            is Operator<*> -> v.invoke()
            is Map<*, *> -> hydrateMap(v as Map<String, Any?>)
            else -> v
        }
    }