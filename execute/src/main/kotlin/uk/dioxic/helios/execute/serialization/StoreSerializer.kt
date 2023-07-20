package uk.dioxic.helios.execute.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.bson.asBsonDecoder
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bson.BsonBoolean
import org.bson.BsonString
import uk.dioxic.helios.execute.model.BooleanStore
import uk.dioxic.helios.execute.model.PathStore
import uk.dioxic.helios.execute.model.Store

object StoreSerializer : KSerializer<Store> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Store", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Store =
        when (val bsonValue = decoder.asBsonDecoder().decodeBsonValue()) {
            is BsonString -> PathStore(bsonValue.value)
            is BsonBoolean -> BooleanStore(bsonValue.value)
            else -> error("${bsonValue.bsonType} not supported")
        }

    override fun serialize(encoder: Encoder, value: Store) {
        when (value) {
            is BooleanStore -> encoder.encodeBoolean(value.persist)
            is PathStore -> encoder.encodeString(value.path)
        }
    }
}