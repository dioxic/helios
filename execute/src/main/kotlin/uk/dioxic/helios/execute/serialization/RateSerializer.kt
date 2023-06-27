package uk.dioxic.helios.execute.serialization

import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import uk.dioxic.helios.execute.model.PeriodRate
import uk.dioxic.helios.execute.model.RampedRate
import uk.dioxic.helios.execute.model.Rate
import uk.dioxic.helios.execute.model.TpsRate

object RateSerializer : JsonContentPolymorphicSerializer<Rate>(Rate::class) {

    override fun selectDeserializer(element: JsonElement) = when {
        "period" in element.jsonObject -> PeriodRate.serializer()
        "rampDuration" in element.jsonObject -> RampedRate.serializer()
        else -> TpsRate.serializer()
    }
}