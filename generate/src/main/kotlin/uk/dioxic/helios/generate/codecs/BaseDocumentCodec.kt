package uk.dioxic.helios.generate.codecs

import org.bson.*
import org.bson.codecs.*
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import org.bson.codecs.jsr310.Jsr310CodecProvider
import uk.dioxic.helios.generate.OperatorTransformer
import java.util.*

sealed interface BaseDocumentCodec<T : MutableMap<String, Any?>> : Codec<T> {
    val registry: CodecRegistry
    val bsonTypeCodecMap: BsonTypeCodecMap
    val valueTransformer: Transformer
    val uuidRepresentation: UuidRepresentation

    fun writeValue(writer: BsonWriter, encoderContext: EncoderContext, value: Any?) {
        if (value == null) {
            writer.writeNull()
        } else {
            val codec = registry.get(value.javaClass)
            encoderContext.encodeWithChildContext(codec, writer, value)
        }
    }

    fun readValue(
        reader: BsonReader,
        decoderContext: DecoderContext,
    ): Any? {
        val bsonType = reader.currentBsonType
        return if (bsonType == BsonType.NULL) {
            reader.readNull()
            null
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
        val defaultBsonTypeClassMap: BsonTypeClassMap = BsonTypeClassMap()
        val defaultRegistry: CodecRegistry = CodecRegistries.fromProviders(
            listOf(
                ValueCodecProvider(),
                Jsr310CodecProvider(),
                CollectionCodecProvider(OperatorTransformer),
                IterableCodecProvider(OperatorTransformer),
                OperatorExecutionCodecProvider(),
                BsonValueCodecProvider(),
                DocumentCodecProvider(),
                MapCodecProvider(OperatorTransformer)
            )
        )
    }

}