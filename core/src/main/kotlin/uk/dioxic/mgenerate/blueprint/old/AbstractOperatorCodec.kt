package uk.dioxic.mgenerate.blueprint.old

import org.bson.*
import org.bson.codecs.*
import org.bson.codecs.configuration.CodecRegistry

abstract class AbstractOperatorCodec<T>(
    private val registry: CodecRegistry = defaultRegistry,
    private val bsonTypeCodecMap: BsonTypeCodecMap = BsonTypeCodecMap(defaultBsonTypeClassMap, defaultRegistry),
    private val valueTransformer: Transformer? = null
) : Codec<T> {

    constructor(registry: CodecRegistry, bsonTypeClassMap: BsonTypeClassMap, valueTransformer: Transformer?) : this(
        registry = registry,
        bsonTypeCodecMap = BsonTypeCodecMap(bsonTypeClassMap, registry),
        valueTransformer = valueTransformer
    )

    override fun encode(writer: BsonWriter, value: T, encoderContext: EncoderContext) {
//        writer.writeStartArray("from")
//        value.from.forEach {
//
//        }
    }

    override fun decode(reader: BsonReader, decoderContext: DecoderContext): T {
        return when (reader.readBsonType()) {
            BsonType.DOCUMENT -> {
                val document = Document()
                reader.readStartDocument()
                while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                    val fieldName = reader.readName()
                    document[fieldName] = readValue(
                        reader,
                        decoderContext,
                        bsonTypeCodecMap,
                        registry
                    )
                }
                reader.readEndDocument()
                // if operator { init operator } else pass through
                build(document)
            }

            else -> error("Can't decode operator")
        }
    }

    abstract fun build(document: Document): T

    abstract fun build(value: Any): T

//    private fun writeValue(writer: BsonWriter, encoderContext: EncoderContext, value: Any?) {
//        if (value == null) {
//            writer.writeNull()
//        } else {
//            val codec: Codec<*> = registry.get(value.javaClass)
//            encoderContext.encodeWithChildContext<Any>(codec, writer, value)
//        }
//    }

}