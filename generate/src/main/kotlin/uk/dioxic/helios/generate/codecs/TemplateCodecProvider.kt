package uk.dioxic.helios.generate.codecs

import org.bson.Transformer
import org.bson.codecs.BsonTypeClassMap
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.configuration.CodecRegistry
import uk.dioxic.helios.generate.OperatorTransformer
import uk.dioxic.helios.generate.Template

data class TemplateCodecProvider @JvmOverloads constructor(
    private val bsonTypeClassMap: BsonTypeClassMap = BsonTypeClassMap(),
    private val valueTransformer: Transformer = OperatorTransformer,
) : CodecProvider {

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(clazz: Class<T>, registry: CodecRegistry): Codec<T>? =
        if (clazz == Template::class.java) {
            TemplateCodec(
                registry = registry,
                bsonTypeClassMap = bsonTypeClassMap,
                valueTransformer = valueTransformer
            ) as Codec<T>
        } else null

}