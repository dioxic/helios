package uk.dioxic.helios.generate.codecs

import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecRegistry
import uk.dioxic.helios.generate.OperatorContext
import uk.dioxic.helios.generate.operators.RootOperator

class RootOperatorCodec(
    private val registry: CodecRegistry,
) : Codec<RootOperator> {

    @Suppress("UNCHECKED_CAST")
    override fun encode(writer: BsonWriter, value: RootOperator, encoderContext: EncoderContext) {
        with(OperatorContext.threadLocal.get()) {
            when (val unwrappedValue = value()) {
                null -> writer.writeNull()
                else -> {
                    val codec = registry[unwrappedValue::class.java] as Codec<Any>
                    codec.encode(writer, unwrappedValue, encoderContext)
                }
            }
        }
    }

    override fun decode(reader: BsonReader, decoderContext: DecoderContext): RootOperator {
        throw UnsupportedOperationException("decode not supported for operator codecs")
    }

    override fun getEncoderClass(): Class<RootOperator> =
        RootOperator::class.java

}