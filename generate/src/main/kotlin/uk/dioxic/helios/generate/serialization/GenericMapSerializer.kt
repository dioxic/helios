package uk.dioxic.helios.generate.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.bson.asBsonDecoder
import kotlinx.serialization.bson.asBsonEncoder
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bson.codecs.*
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import org.bson.codecs.jsr310.Jsr310CodecProvider
import uk.dioxic.helios.generate.OperatorTransformer
import uk.dioxic.helios.generate.codecs.DocumentCodecProvider
import uk.dioxic.helios.generate.codecs.EncodeContextCodecProvider
import uk.dioxic.helios.generate.codecs.WrappedCodecProvider

object GenericMapSerializer : KSerializer<Map<*, *>> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Map", PrimitiveKind.STRING)

    val defaultBsonTypeClassMap: BsonTypeClassMap = BsonTypeClassMap()
    private val registry: CodecRegistry = CodecRegistries.fromProviders(
        listOf(
            ValueCodecProvider(),
            Jsr310CodecProvider(),
            CollectionCodecProvider(OperatorTransformer),
            IterableCodecProvider(OperatorTransformer),
            WrappedCodecProvider(),
            EncodeContextCodecProvider(),
            BsonValueCodecProvider(),
            DocumentCodecProvider(),
            MapCodecProvider(OperatorTransformer)
        )
    )
    private val codec = MapCodecProvider()[Map::class.java, registry]

    override fun deserialize(decoder: Decoder): Map<*, *> =
        codec.decode(decoder.asBsonDecoder().reader(), DecoderContext.builder().build())

    override fun serialize(encoder: Encoder, value: Map<*, *>) {
        codec.encode(encoder.asBsonEncoder().writer(), value, EncoderContext.builder().build())
    }
}
