package uk.dioxic.mgenerate.blueprint

import org.bson.Document
import org.bson.Transformer
import org.bson.codecs.*
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.configuration.CodecRegistry
import org.bson.types.CodeWithScope

class TemplateDocumentCodecProvider @JvmOverloads constructor(
    private val bsonTypeClassMap: BsonTypeClassMap = BsonTypeClassMap(),
    private val valueTransformer: Transformer? = null
) : CodecProvider {

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(clazz: Class<T>, registry: CodecRegistry): Codec<T>? {
        if (clazz == CodeWithScope::class.java) {
            return CodeWithScopeCodec(registry.get(Document::class.java)) as Codec<T>
        }
        return if (clazz == Document::class.java) {
            TemplateDocumentCodec(registry, bsonTypeClassMap, valueTransformer) as Codec<T>
        } else null
    }

}