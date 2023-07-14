package uk.dioxic.helios.generate.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.bson.BsonValueSerializer
import kotlinx.serialization.bson.buildBsonDocument
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bson.BsonDocument
import org.bson.BsonString
import org.bson.codecs.DecoderContext
import uk.dioxic.helios.generate.Template
import uk.dioxic.helios.generate.codecs.DocumentCodec
import uk.dioxic.helios.generate.putRoot

object TemplateSerializer : KSerializer<Template> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("TemplateSerializer", PrimitiveKind.STRING)

    private val documentCodec = DocumentCodec()

    override fun deserialize(decoder: Decoder): Template {
        val bsonValue = decoder.decodeSerializableValue(BsonValueSerializer)
        val bsonDoc = when (bsonValue) {
            is BsonDocument -> bsonValue
            is BsonString -> buildBsonDocument {
                putRoot(bsonValue.value)
            }
            else -> error("deserialization from ${bsonValue.bsonType} not supported")
        }
        val exeutionDoc = documentCodec.decode(bsonDoc.asBsonReader(), DecoderContext.builder().build())

        return Template(exeutionDoc, bsonValue)
    }

    override fun serialize(encoder: Encoder, value: Template) {
        requireNotNull(value.definition) {
            "template definition not set"
        }
        encoder.encodeSerializableValue(BsonValueSerializer, value.definition)
    }
}