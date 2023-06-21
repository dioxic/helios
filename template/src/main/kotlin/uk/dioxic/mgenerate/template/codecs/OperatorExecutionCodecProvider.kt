package uk.dioxic.mgenerate.template.codecs

import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.configuration.CodecRegistry
import uk.dioxic.mgenerate.template.operators.Operator

class OperatorExecutionCodecProvider: CodecProvider {

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> get(clazz: Class<T>, registry: CodecRegistry): Codec<T>? {
        if (Operator::class.java.isAssignableFrom(clazz)) {
            return uk.dioxic.mgenerate.template.codecs.OperatorExecutionCodec(registry) as Codec<T>
        }
        return null
    }

}