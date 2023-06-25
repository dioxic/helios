package uk.dioxic.mgenerate.execute.serialization

import com.mongodb.WriteConcern
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.*
import java.util.concurrent.TimeUnit

object WriteConcernSerializer : KSerializer<WriteConcern> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("WriteConcern") {
            element<String>("w")
            element<Boolean>("j")
            element<Int>("wtimeout")
        }

    override fun deserialize(decoder: Decoder): WriteConcern =
        when (val jsonElement = decoder.asJsonDecoder().decodeJsonElement()) {
            is JsonObject -> {
                deserializeW(jsonElement["w"]).let { wc ->
                    wc.withJournal(jsonElement["j"]?.jsonPrimitive?.boolean).let {wc ->
                        jsonElement["wtimeout"]?.let {
                            wc.withWTimeout(it.jsonPrimitive.long, TimeUnit.MILLISECONDS)
                        } ?: wc
                    }
                }
            }
            else -> deserializeW(jsonElement)
        }

    private fun deserializeW(w: JsonElement?) =
        when (w) {
            is JsonPrimitive -> when {
                w.isString -> WriteConcern(w.content)
                else -> WriteConcern(w.int)
            }

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