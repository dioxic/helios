package uk.dioxic.mgenerate.serialization

import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import uk.dioxic.mgenerate.worker.model.PeriodRate
import uk.dioxic.mgenerate.worker.model.RampedRate
import uk.dioxic.mgenerate.worker.model.Rate
import uk.dioxic.mgenerate.worker.model.TpsRate

object RateSerializer : JsonContentPolymorphicSerializer<Rate>(Rate::class) {

    override fun selectDeserializer(element: JsonElement) = when {
        "period" in element.jsonObject -> PeriodRate.serializer()
        "rampDuration" in element.jsonObject -> RampedRate.serializer()
        else -> TpsRate.serializer()
    }
}