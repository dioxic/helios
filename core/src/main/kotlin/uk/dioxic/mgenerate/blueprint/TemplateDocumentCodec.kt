package uk.dioxic.mgenerate.blueprint

import org.bson.BsonReader
import org.bson.Document
import org.bson.Transformer
import org.bson.codecs.*
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import uk.dioxic.mgenerate.blueprint.old.OperatorCodecProvider
import uk.dioxic.mgenerate.blueprint.old.defaultBsonTypeClassMap

class TemplateDocumentCodec(
    private val registry: CodecRegistry = CodecRegistries.fromProviders(
        listOf(
            ValueCodecProvider(),
            CollectionCodecProvider(), IterableCodecProvider(), OperatorCodecProvider(),
            BsonValueCodecProvider(), TemplateDocumentCodecProvider(), MapCodecProvider()
        )
    ),
    bsonTypeClassMap: BsonTypeClassMap = defaultBsonTypeClassMap,
    valueTransformer: Transformer? = null
) : DocumentCodec(registry, bsonTypeClassMap, valueTransformer) {

    private val bsonTypeCodecMap: BsonTypeCodecMap

    init {
        bsonTypeCodecMap = BsonTypeCodecMap(bsonTypeClassMap, registry)
    }

    override fun decode(reader: BsonReader, decoderContext: DecoderContext): Document {
        val document = Document()

//        reader.readStartDocument()
//        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
//            val fieldName = reader.readName()
//            document[fieldName] = if (OperatorRegistry.hasAlias(fieldName)) {
//                registry.get(OperatorRegistry.getClass(fieldName))
//                    .decode(reader, decoderContext)
//            } else {
//                readValue(
//                    reader,
//                    decoderContext,
//                    bsonTypeCodecMap,
//                    registry
//                )
//            }
//        }

        reader.readEndDocument()

        return document
    }

}