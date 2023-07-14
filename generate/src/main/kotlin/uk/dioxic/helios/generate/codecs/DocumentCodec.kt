package uk.dioxic.helios.generate.codecs

import org.bson.*
import org.bson.codecs.*
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import org.bson.codecs.jsr310.Jsr310CodecProvider
import uk.dioxic.helios.generate.OperatorTransformer
import uk.dioxic.helios.generate.operators.rootKey
import java.util.*
import uk.dioxic.helios.generate.codecs.DocumentCodecProvider as HeliosDocumentCodecProvider

class DocumentCodec(
    val registry: CodecRegistry = defaultRegistry,
    bsonTypeClassMap: BsonTypeClassMap = defaultBsonTypeClassMap,
    private val bsonTypeCodecMap: BsonTypeCodecMap = BsonTypeCodecMap(bsonTypeClassMap, registry),
    private val idGenerator: IdGenerator = ObjectIdGenerator(),
    private val valueTransformer: Transformer = OperatorTransformer,
    private val uuidRepresentation: UuidRepresentation = UuidRepresentation.UNSPECIFIED
) : Codec<Document>, OverridableUuidRepresentationCodec<Document> {

    override fun getEncoderClass() =
        Document::class.java

    override fun encode(writer: BsonWriter, document: Document, encoderContext: EncoderContext) {
        document[rootKey]?.let { root ->
            val codec = registry.get(root.javaClass)
            codec.encode(writer, root, encoderContext)
            return
        }

        writer.writeStartDocument()

        beforeFields(writer, encoderContext, document)

        document.forEach { (key, value) ->
            if (!skipField(encoderContext, key)) {
                writer.writeName(key)
                writeValue(writer, encoderContext, value)
            }
        }

        writer.writeEndDocument()
    }

    override fun decode(reader: BsonReader, decoderContext: DecoderContext): Document {
        val document = Document()

        reader.readStartDocument()
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            val fieldName = reader.readName()
            document[fieldName] = readValue(reader, decoderContext)
        }

        reader.readEndDocument()

        return document
    }

    private fun beforeFields(
        bsonWriter: BsonWriter,
        encoderContext: EncoderContext,
        document: Document
    ) {
        if (encoderContext.isEncodingCollectibleDocument) {
            (document[idFieldName] ?: idGenerator.generate()).let { id ->
                bsonWriter.writeName(idFieldName)
                writeValue(bsonWriter, encoderContext, id)
            }
        }
    }

    private fun skipField(encoderContext: EncoderContext, key: String): Boolean =
        encoderContext.isEncodingCollectibleDocument && key == idFieldName

    override fun withUuidRepresentation(uuidRepresentation: UuidRepresentation): Codec<Document> =
        if (uuidRepresentation == this.uuidRepresentation) {
            this
        } else {
            DocumentCodec(
                registry = registry,
                bsonTypeCodecMap = bsonTypeCodecMap,
                idGenerator = idGenerator,
                uuidRepresentation = uuidRepresentation,
                valueTransformer = valueTransformer
            )
        }

    private fun writeValue(writer: BsonWriter, encoderContext: EncoderContext, value: Any?) {
        if (value == null) {
            writer.writeNull()
        } else {
            val codec = registry.get(value.javaClass)
            encoderContext.encodeWithChildContext(codec, writer, value)
        }
    }

    private fun readValue(
        reader: BsonReader,
        decoderContext: DecoderContext,
    ): Any? {
        val bsonType = reader.currentBsonType
        return if (bsonType == BsonType.NULL) {
            reader.readNull()
            null to null
        } else {
            var codec = bsonTypeCodecMap[bsonType]

            if (bsonType == BsonType.BINARY && reader.peekBinarySize() == 16) {
                when (reader.peekBinarySubType()) {
                    3.toByte() -> {
                        if (uuidRepresentation == UuidRepresentation.JAVA_LEGACY
                            || uuidRepresentation == UuidRepresentation.C_SHARP_LEGACY
                            || uuidRepresentation == UuidRepresentation.PYTHON_LEGACY
                        ) {
                            codec = registry.get(UUID::class.java)
                        }
                    }

                    4.toByte() -> {
                        if (uuidRepresentation == UuidRepresentation.STANDARD) {
                            codec = registry.get(UUID::class.java)
                        }
                    }

                    else -> {}
                }
            }
            valueTransformer.transform(codec.decode(reader, decoderContext))
        }
    }

    companion object {
        const val idFieldName: String = "_id"
        val defaultBsonTypeClassMap: BsonTypeClassMap = BsonTypeClassMap()
        val defaultRegistry: CodecRegistry = CodecRegistries.fromProviders(
            listOf(
                ValueCodecProvider(),
                Jsr310CodecProvider(),
                CollectionCodecProvider(OperatorTransformer),
                IterableCodecProvider(OperatorTransformer),
                WrappedCodecProvider(),
                BsonValueCodecProvider(),
                HeliosDocumentCodecProvider(),
                MapCodecProvider(OperatorTransformer)
            )
        )
    }

}