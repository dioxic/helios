package uk.dioxic.mgenerate.codecs

import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecRegistry
import uk.dioxic.mgenerate.operators.Operator

class OperatorExecutionCodec(
    private val registry: CodecRegistry
): Codec<Operator<*>> {

    @Suppress("UNCHECKED_CAST")
    override fun encode(writer: BsonWriter, value: Operator<*>, encoderContext: EncoderContext) {
        val unwrappedValue = value()
        val codec = registry[unwrappedValue::class.java] as Codec<Any>
        encoderContext.encodeWithChildContext(codec, writer, unwrappedValue)
    }

    override fun getEncoderClass(): Class<Operator<*>> =
        Operator::class.java

    override fun decode(reader: BsonReader, decoderContext: DecoderContext): Operator<*> {
        error("Decode not implemented")
    }
}