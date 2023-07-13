package uk.dioxic.helios.generate.codecs

import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.configuration.CodecRegistry
import uk.dioxic.helios.generate.Wrapped

class WrappedCodecProvider: CodecProvider {

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> get(clazz: Class<T>, registry: CodecRegistry): Codec<T>? {
        if (Wrapped::class.java.isAssignableFrom(clazz)) {
            return WrappedCodec(registry) as Codec<T>
        }
        return null
    }

}