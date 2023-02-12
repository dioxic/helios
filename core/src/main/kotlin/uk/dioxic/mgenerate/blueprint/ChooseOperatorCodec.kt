package uk.dioxic.mgenerate.blueprint

import org.bson.*
import org.bson.codecs.*
import org.bson.codecs.configuration.CodecRegistry

class ChooseOperatorCodec(
    registry: CodecRegistry = defaultRegistry,
    bsonTypeCodecMap: BsonTypeCodecMap = BsonTypeCodecMap(defaultBsonTypeClassMap, defaultRegistry),
    valueTransformer: Transformer? = null
) : AbstractOperatorCodec<ChooseOperator>(
    registry = registry,
    bsonTypeCodecMap = bsonTypeCodecMap,
    valueTransformer = valueTransformer
) {

    constructor(registry: CodecRegistry, bsonTypeClassMap: BsonTypeClassMap, valueTransformer: Transformer?) : this(
        registry = registry,
        bsonTypeCodecMap = BsonTypeCodecMap(bsonTypeClassMap, registry),
        valueTransformer = valueTransformer
    )

    override fun getEncoderClass(): Class<ChooseOperator> =
        ChooseOperator::class.java

    override fun build(document: Document) =
        ChooseOperator(
            document.getList("from", Any::class.java, emptyList()),
            document.getList("weights", Int::class.java),
        )

    @Suppress("UNCHECKED_CAST")
    override fun build(value: Any) =
        ChooseOperator(value as List<Any>)

//    private fun writeValue(writer: BsonWriter, encoderContext: EncoderContext, value: Any?) {
//        if (value == null) {
//            writer.writeNull()
//        } else {
//            val codec: Codec<*> = registry.get(value.javaClass)
//            encoderContext.encodeWithChildContext<Any>(codec, writer, value)
//        }
//    }

}