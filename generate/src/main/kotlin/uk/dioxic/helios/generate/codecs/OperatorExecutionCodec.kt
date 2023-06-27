package uk.dioxic.helios.generate.codecs

import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecRegistry
import uk.dioxic.helios.generate.operators.Operator

class OperatorExecutionCodec(
    private val registry: CodecRegistry
) : Codec<Operator<*>> {

    @Suppress("UNCHECKED_CAST")
    override fun encode(writer: BsonWriter, value: Operator<*>, encoderContext: EncoderContext) {
        when (val unwrappedValue = value()) {
            null -> writer.writeNull()
            else -> {
                val codec = registry[unwrappedValue::class.java] as Codec<Any>
                encoderContext.encodeWithChildContext(codec, writer, unwrappedValue)
            }
        }
    }

    override fun getEncoderClass(): Class<Operator<*>> =
        Operator::class.java

    override fun decode(reader: BsonReader, decoderContext: DecoderContext): Operator<*> {
        error("Decode not implemented")
    }
}