package uk.dioxic.mgenerate.worker.model

import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

object FixedRateSerializer : JsonContentPolymorphicSerializer<FixedRate>(FixedRate::class) {

    override fun selectDeserializer(element: JsonElement) = when {
        "tps" in element.jsonObject -> TpsRate.serializer()
        "period" in element.jsonObject -> PeriodRate.serializer()
        else -> error("cannot deserialize $element")
    }
}