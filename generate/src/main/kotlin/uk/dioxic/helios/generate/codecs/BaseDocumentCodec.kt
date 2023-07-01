package uk.dioxic.helios.generate.codecs

import org.bson.*
import org.bson.codecs.*
import org.bson.codecs.configuration.CodecRegistry
import uk.dioxic.helios.generate.OperatorContext
import uk.dioxic.helios.generate.Wrapped
import uk.dioxic.helios.generate.getOperatorKey
import uk.dioxic.helios.generate.operators.RootOperator
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

    @Suppress("UNCHECKED_CAST")
    fun Map<String, Any?>.resolveRoot() =
        this[rootOperatorKey]?.let { value ->
            require(size == 1 || (size == 2 && this.containsKey(idFieldName))) {
                "document should only have one top-level field when $rootOperatorKey is present"
            }
            when (value) {
                is Wrapped<*> -> with(OperatorContext.threadLocal.get()) {
                    value.invoke()
                }

                else -> value
            }.also {
                require(it is Map<*, *>) {
                    "$rootOperatorKey must resolve to a map but was '$it'"
                }
            } as Map<String,Any?>
        } ?: this

    companion object {
        const val idFieldName: String = "_id"
        private val rootOperatorKey = getOperatorKey<RootOperator>()
        val defaultBsonTypeClassMap: BsonTypeClassMap = BsonTypeClassMap()
    }

}