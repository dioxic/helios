package uk.dioxic.mgenerate.worker.model

import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

object RateSerializer : JsonContentPolymorphicSerializer<Rate>(Rate::class) {

    override fun selectDeserializer(element: JsonElement) = when {
        "period" in element.jsonObject -> PeriodRate.serializer()
        "rampDuration" in element.jsonObject -> RampedRate.serializer()
        else -> TpsRate.serializer()
    }
}