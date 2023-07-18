package uk.dioxic.helios.generate.codecs

import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.Document
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecRegistry
import uk.dioxic.helios.generate.EncodeContext
import uk.dioxic.helios.generate.OperatorContext

class EncodeContextCodec(
    private val registry: CodecRegistry
): Codec<EncodeContext> {
    override fun encode(writer: BsonWriter, value: EncodeContext, encoderContext: EncoderContext) {
        OperatorContext.threadLocal.set(value.operatorContext)
        val codec = registry[Document::class.java]
        codec.encode(writer, value.document, encoderContext)
    }

    override fun getEncoderClass(): Class<EncodeContext> =
        EncodeContext::class.java

    override fun decode(reader: BsonReader, decoderContext: DecoderContext): EncodeContext {
        throw UnsupportedOperationException()
    }
}