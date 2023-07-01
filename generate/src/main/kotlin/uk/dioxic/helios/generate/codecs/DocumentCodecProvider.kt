package uk.dioxic.helios.generate.codecs

import org.bson.Document
import org.bson.Transformer
import org.bson.codecs.BsonTypeClassMap
import org.bson.codecs.CodeWithScopeCodec
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.configuration.CodecRegistry
import org.bson.types.CodeWithScope
import uk.dioxic.helios.generate.OperatorTransformer

data class DocumentCodecProvider @JvmOverloads constructor(
    private val bsonTypeClassMap: BsonTypeClassMap = BsonTypeClassMap(),
    private val valueTransformer: Transformer = OperatorTransformer,
) : CodecProvider {

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(clazz: Class<T>, registry: CodecRegistry): Codec<T>? {
        if (clazz == CodeWithScope::class.java) {
            return CodeWithScopeCodec(registry.get(Document::class.java)) as Codec<T>
        }
        return if (clazz == Document::class.java) {
            DocumentCodec(
                registry = registry,
                bsonTypeClassMap = bsonTypeClassMap,
                valueTransformer = valueTransformer
            ) as Codec<T>
        } else null
    }

}