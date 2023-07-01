package uk.dioxic.helios.generate.codecs

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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
import uk.dioxic.helios.generate.operators.ObjectIdOperator
import uk.dioxic.helios.generate.codecs.DocumentCodecProvider as HeliosDocumentCodecProvider

class TemplateCodec(
    override val registry: CodecRegistry = defaultRegistry,
    bsonTypeClassMap: BsonTypeClassMap = defaultBsonTypeClassMap,
    override val bsonTypeCodecMap: BsonTypeCodecMap = BsonTypeCodecMap(bsonTypeClassMap, registry),
    private val idGenerator: Operator<*> = ObjectIdOperator(),
    override val valueTransformer: Transformer = OperatorTransformer,
    override val uuidRepresentation: UuidRepresentation = UuidRepresentation.UNSPECIFIED
) : BaseDocumentCodec<Template>, CollectibleCodec<Template>, OverridableUuidRepresentationCodec<Template> {

    private val idFieldName = "_id"

    override fun encode(writer: BsonWriter, template: Template, encoderContext: EncoderContext) {
//        val t = template
//
//        template[getOperatorKey<RootOperator>()].let {
//            if (it != null && it is RootOperator) {
//                it.value.invoke()
//            }
//        }
//
//        if (template.containsKey(getOperatorKey<RootOperator>())) {
//
//        }

        writer.writeStartDocument()

        beforeFields(writer, encoderContext, template)

        template.forEach { key, value ->
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
        val map = mutableMapOf<String, Any?>()

        reader.readStartDocument()
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            val fieldName = reader.readName()
            map[fieldName] = readValue(reader, decoderContext)
        }

        reader.readEndDocument()

        return Template(map)
    }

    fun decode(definition: JsonObject): Template {
        val jsonReader = JsonReader(Json.encodeToString(definition))
        val document = decode(jsonReader, DecoderContext.builder().build())
        return Template(document, definition)
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

    private fun beforeFields(bsonWriter: BsonWriter, encoderContext: EncoderContext, template: Template) {
        if (encoderContext.isEncodingCollectibleDocument && template.containsKey(idFieldName)) {
            bsonWriter.writeName(idFieldName)
            writeValue(bsonWriter, encoderContext, template[idFieldName])
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