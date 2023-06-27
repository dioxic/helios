package uk.dioxic.helios.generate.codecs

import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.configuration.CodecRegistry
import uk.dioxic.helios.generate.operators.Operator

class OperatorExecutionCodecProvider: CodecProvider {

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> get(clazz: Class<T>, registry: CodecRegistry): Codec<T>? {
        if (Operator::class.java.isAssignableFrom(clazz)) {
            return OperatorExecutionCodec(registry) as Codec<T>
        }
        return null
    }

}