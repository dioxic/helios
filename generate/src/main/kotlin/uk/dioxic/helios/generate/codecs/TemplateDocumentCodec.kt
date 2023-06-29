package uk.dioxic.helios.generate.codecs

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.bson.BsonReader
import org.bson.BsonValue
import org.bson.BsonWriter
import org.bson.codecs.*
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import org.bson.codecs.jsr310.Jsr310CodecProvider
import org.bson.json.JsonReader
import uk.dioxic.helios.generate.OperatorTransformer
import uk.dioxic.helios.generate.Template
import uk.dioxic.helios.generate.operators.ObjectIdOperator

class TemplateDocumentCodec(
    registry: CodecRegistry = CodecRegistries.fromProviders(
        listOf(
            ValueCodecProvider(), Jsr310CodecProvider(),
            CollectionCodecProvider(), IterableCodecProvider(), OperatorExecutionCodecProvider(),
            BsonValueCodecProvider(), DocumentCodecProvider(OperatorTransformer()), MapCodecProvider()
        )
    ),
    bsonTypeClassMap: BsonTypeClassMap = BsonTypeClassMap(),
) : CollectibleCodec<Template> {

    private val idFieldName = "_id"
    private val documentCodec = DocumentCodec(registry, bsonTypeClassMap, OperatorTransformer())

    override fun encode(writer: BsonWriter, value: Template, encoderContext: EncoderContext) {
        documentCodec.encode(writer, value, encoderContext)
    }

    override fun getEncoderClass(): Class<Template> =
        Template::class.java

    override fun decode(reader: BsonReader, decoderContext: DecoderContext) =
        Template(documentCodec.decode(reader, decoderContext))

    fun decode(definition: JsonObject): Template {
        val jsonReader = JsonReader(Json.encodeToString(definition))
        val document = documentCodec.decode(jsonReader, DecoderContext.builder().build())
        return Template(document, definition)
    }

    override fun getDocumentId(template: Template): BsonValue =
        documentCodec.getDocumentId(template)

    override fun documentHasId(template: Template): Boolean =
        template.containsKey(idFieldName)

    override fun generateIdIfAbsentFromDocument(template: Template) = template.apply {
        if (!documentHasId(template)) {
            template[idFieldName] = ObjectIdOperator()
        }
    }

}