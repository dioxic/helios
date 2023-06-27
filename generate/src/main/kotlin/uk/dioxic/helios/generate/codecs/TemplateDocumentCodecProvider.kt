package uk.dioxic.helios.generate.codecs

import org.bson.Document
import org.bson.codecs.BsonTypeClassMap
import org.bson.codecs.CodeWithScopeCodec
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.configuration.CodecRegistry
import org.bson.types.CodeWithScope
import uk.dioxic.helios.generate.Template

class TemplateDocumentCodecProvider @JvmOverloads constructor(
    private val bsonTypeClassMap: BsonTypeClassMap = BsonTypeClassMap()
) : CodecProvider {

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(clazz: Class<T>, registry: CodecRegistry): Codec<T>? {
        if (clazz == CodeWithScope::class.java) {
            return CodeWithScopeCodec(registry.get(Document::class.java)) as Codec<T>
        }
        return if (clazz == Template::class.java) {
            TemplateDocumentCodec(registry, bsonTypeClassMap) as Codec<T>
        } else null
    }

}