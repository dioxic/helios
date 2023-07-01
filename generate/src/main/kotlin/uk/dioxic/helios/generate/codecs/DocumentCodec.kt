package uk.dioxic.helios.generate.codecs

import org.bson.*
import org.bson.codecs.BsonTypeClassMap
import org.bson.codecs.BsonTypeCodecMap
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecRegistry
import uk.dioxic.helios.generate.OperatorTransformer
import uk.dioxic.helios.generate.codecs.BaseDocumentCodec.Companion.defaultBsonTypeClassMap
import uk.dioxic.helios.generate.codecs.BaseDocumentCodec.Companion.defaultRegistry

class DocumentCodec(
    override val registry: CodecRegistry = defaultRegistry,
    bsonTypeClassMap: BsonTypeClassMap = defaultBsonTypeClassMap,
    override val bsonTypeCodecMap: BsonTypeCodecMap = BsonTypeCodecMap(bsonTypeClassMap, registry),
    override val valueTransformer: Transformer = OperatorTransformer,
    override val uuidRepresentation: UuidRepresentation = UuidRepresentation.UNSPECIFIED
) : BaseDocumentCodec<Document> {

    override fun getEncoderClass() =
        Document::class.java

    override fun encode(writer: BsonWriter, document: Document, encoderContext: EncoderContext) {
        writer.writeStartDocument()

        document.forEach { key, value ->
            writer.writeName(key)
            writeValue(writer, encoderContext, value)
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

}