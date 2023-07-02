package uk.dioxic.helios.generate.codecs

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.bson.*
import org.bson.codecs.*
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import org.bson.codecs.jsr310.Jsr310CodecProvider
import org.bson.json.JsonReader
import uk.dioxic.helios.generate.Operator
import uk.dioxic.helios.generate.OperatorTransformer
import uk.dioxic.helios.generate.Template
import uk.dioxic.helios.generate.codecs.BaseDocumentCodec.Companion.defaultBsonTypeClassMap
import uk.dioxic.helios.generate.codecs.BaseDocumentCodec.Companion.idFieldName
import uk.dioxic.helios.generate.codecs.BaseDocumentCodec.Companion.rootOperatorKey
import uk.dioxic.helios.generate.operators.ObjectIdOperator
import uk.dioxic.helios.generate.putRootOperator
import uk.dioxic.helios.generate.codecs.DocumentCodecProvider as HeliosDocumentCodecProvider

class TemplateCodec(
    override val registry: CodecRegistry = defaultRegistry,
    bsonTypeClassMap: BsonTypeClassMap = defaultBsonTypeClassMap,
    override val bsonTypeCodecMap: BsonTypeCodecMap = BsonTypeCodecMap(bsonTypeClassMap, registry),
    private val idGenerator: Operator<*> = ObjectIdOperator(),
    override val valueTransformer: Transformer = OperatorTransformer,
    override val uuidRepresentation: UuidRepresentation = UuidRepresentation.UNSPECIFIED
) : BaseDocumentCodec<Template>, CollectibleCodec<Template>, OverridableUuidRepresentationCodec<Template> {

    override fun encode(writer: BsonWriter, template: Template, encoderContext: EncoderContext) {
        writer.writeStartDocument()

        val root = template.resolveRoot()

        beforeFields(writer, encoderContext, template, root)

        root.forEach { (key, value) ->
            if (!skipField(encoderContext, key)) {
                writer.writeName(key)
                writeValue(writer, encoderContext, value)
            }
        }

        writer.writeEndDocument()
    }

    override fun getEncoderClass(): Class<Template> =
        Template::class.java

    override fun decode(reader: BsonReader, decoderContext: DecoderContext): Template {
        val tMap = mutableMapOf<String, Any?>()
        val dMap = mutableMapOf<String, Any?>()

        when (reader.currentBsonType) {
            BsonType.DOCUMENT -> {
                reader.readStartDocument()
                while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                    val name = reader.readName()
                    readValue(reader, decoderContext).also { (original, transformed) ->
                        dMap[name] = original
                        tMap[name] = transformed
                    }
                }
                reader.readEndDocument()
            }
            BsonType.STRING -> {
                readValue(reader, decoderContext).also { (original, transformed) ->
                    dMap[rootOperatorKey] = original
                    tMap[rootOperatorKey] = transformed
                }
            }
            else -> error("Cannot decode ${reader.readBsonType()} to a Template")
        }

        return Template(tMap, dMap)
    }

    fun decode(definition: JsonElement): Template {
        val document = when (definition)  {
            is JsonObject -> {
                val jsonReader = JsonReader(Json.encodeToString(definition))
                decode(jsonReader, DecoderContext.builder().build())
            }
            is JsonPrimitive -> {
                require(definition.isString) {
                    "template root definition must be a string but was $definition"
                }
                decode(buildJsonObject {
                    putRootOperator(definition.content)
                })
            }
            else -> error("template root definition must be an object or a string but was $definition")
        }

        return Template(document)
    }

    override fun getDocumentId(template: Template): BsonValue {
        require(documentHasId(template)) {
            "The document does not contain an _id"
        }

        val id = template[idFieldName]
        if (id is BsonValue) {
            return id
        }

        val idHoldingDocument = BsonDocument()
        val writer = BsonDocumentWriter(idHoldingDocument)
        writer.writeStartDocument()
        writer.writeName(idFieldName)
        writeValue(writer, EncoderContext.builder().build(), id)
        writer.writeEndDocument()
        return idHoldingDocument[idFieldName]!!
    }

    override fun documentHasId(template: Template): Boolean =
        template.containsKey(idFieldName)

    override fun generateIdIfAbsentFromDocument(template: Template) = template.apply {
        if (!documentHasId(template)) {
            template[idFieldName] = idGenerator
        }
    }

    private fun beforeFields(
        bsonWriter: BsonWriter,
        encoderContext: EncoderContext,
        template: Template,
        root: Map<String, Any?>
    ) {
        if (encoderContext.isEncodingCollectibleDocument) {
            (root[idFieldName] ?: template[idFieldName])?.let { id ->
                bsonWriter.writeName(idFieldName)
                writeValue(bsonWriter, encoderContext, id)
            }
        }
    }

    private fun skipField(encoderContext: EncoderContext, key: String): Boolean =
        encoderContext.isEncodingCollectibleDocument && key == idFieldName

    override fun withUuidRepresentation(uuidRepresentation: UuidRepresentation): Codec<Template> =
        if (uuidRepresentation == this.uuidRepresentation) {
            this
        } else {
            TemplateCodec(
                registry = registry,
                bsonTypeCodecMap = bsonTypeCodecMap,
                idGenerator = idGenerator,
                uuidRepresentation = uuidRepresentation,
                valueTransformer = valueTransformer
            )
        }

    companion object {
        val defaultRegistry: CodecRegistry = CodecRegistries.fromProviders(
            listOf(
                ValueCodecProvider(),
                Jsr310CodecProvider(),
                CollectionCodecProvider(OperatorTransformer),
                IterableCodecProvider(OperatorTransformer),
                OperatorExecutionCodecProvider(),
                BsonValueCodecProvider(),
                TemplateCodecProvider(),
                HeliosDocumentCodecProvider(),
                MapCodecProvider(OperatorTransformer)
            )
        )
    }

}