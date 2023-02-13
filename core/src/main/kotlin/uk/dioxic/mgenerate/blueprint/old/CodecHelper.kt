package uk.dioxic.mgenerate.blueprint.old

import org.bson.BsonReader
import org.bson.BsonType
import org.bson.Transformer
import org.bson.codecs.*
import org.bson.codecs.configuration.CodecConfigurationException
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*

val defaultBsonTypeClassMap = BsonTypeClassMap()
val defaultRegistry: CodecRegistry = CodecRegistries.fromProviders(
    listOf(
        ValueCodecProvider(),
        CollectionCodecProvider(), IterableCodecProvider(),
        BsonValueCodecProvider(), DocumentCodecProvider(), MapCodecProvider()
    )
)

fun readValue(
    reader: BsonReader, decoderContext: DecoderContext?,
    bsonTypeCodecMap: BsonTypeCodecMap,
    registry: CodecRegistry, valueTransformer: Transformer = Transformer { it }
): Any? {
    val bsonType = reader.currentBsonType
    return if (bsonType == BsonType.NULL) {
        reader.readNull()
        null
    } else {
        var codec = bsonTypeCodecMap[bsonType]
        if (bsonType == BsonType.BINARY && reader.peekBinarySize() == 16) {
            if (reader.peekBinarySubType() == 3.toByte() ||
                reader.peekBinarySubType() == 4.toByte()
            ) {
                codec = registry.get(UUID::class.java)
            }
        }
        valueTransformer.transform(codec.decode(reader, decoderContext))
    }
}

fun getClass(codecRegistry: CodecRegistry, type: Type): Codec<*>? {
    return when (type) {
        is Class<*> ->
            codecRegistry[type]

        is ParameterizedType -> {
            codecRegistry[type.rawType as Class<*>, listOf(*type.actualTypeArguments)]
        }

        else ->
            throw CodecConfigurationException("Unsupported generic type of container: $type")
    }
}