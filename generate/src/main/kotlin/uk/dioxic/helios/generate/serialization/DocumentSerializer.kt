package uk.dioxic.helios.generate.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.bson.asBsonDecoder
import kotlinx.serialization.bson.asBsonEncoder
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bson.Document
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import uk.dioxic.helios.generate.codecs.DocumentCodec

object DocumentSerializer : KSerializer<Document> {
    private val documentCodec = DocumentCodec()
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Document", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Document =
        documentCodec.decode(decoder.asBsonDecoder().reader(), DecoderContext.builder().build())

    override fun serialize(encoder: Encoder, value: Document) {
        documentCodec.encode(encoder.asBsonEncoder().writer(), value, EncoderContext.builder().build())
    }
}