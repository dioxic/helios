package uk.dioxic.helios.execute.serialization

import com.mongodb.WriteConcern
import kotlinx.serialization.KSerializer
import kotlinx.serialization.bson.BsonValueSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import org.bson.BsonDocument
import org.bson.BsonNumber
import org.bson.BsonString
import org.bson.BsonValue
import java.util.concurrent.TimeUnit

object WriteConcernSerializer : KSerializer<WriteConcern> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("WriteConcern") {
            element<String>("w")
            element<Boolean>("j")
            element<Int>("wtimeout")
        }

    override fun deserialize(decoder: Decoder): WriteConcern =
        when (val bsonValue = decoder.decodeSerializableValue(BsonValueSerializer)) {
            is BsonDocument -> {
                deserializeW(bsonValue["w"]).let { wc ->
                    wc.withJournal(bsonValue["j"]?.asBoolean()?.value).let { wcj ->
                        bsonValue["wtimeout"]?.let {
                            wcj.withWTimeout(it.asNumber().longValue(), TimeUnit.MILLISECONDS)
                        } ?: wcj
                    }
                }
            }

            else -> deserializeW(bsonValue)
        }

    private fun deserializeW(w: BsonValue?) =
        when (w) {
            is BsonString -> WriteConcern(w.value)
            is BsonNumber -> WriteConcern(w.intValue())
            else -> WriteConcern.MAJORITY
        }

    override fun serialize(encoder: Encoder, value: WriteConcern) {
        encoder.encodeStructure(descriptor) {
            when (value.wObject) {
                is String -> encodeStringElement(descriptor, 0, value.wString)
                is Int -> encodeIntElement(descriptor, 0, value.w)
            }
            value.journal?.also {
                encodeBooleanElement(descriptor, 1, it)
            }
            value.getWTimeout(TimeUnit.MILLISECONDS)?.also {
                encodeIntElement(descriptor, 2, it)
            }
        }
    }

}