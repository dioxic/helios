package uk.dioxic.helios.execute.serialization

import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import uk.dioxic.helios.execute.model.FixedRate
import uk.dioxic.helios.execute.model.PeriodRate
import uk.dioxic.helios.execute.model.TpsRate

object FixedRateSerializer : JsonContentPolymorphicSerializer<FixedRate>(FixedRate::class) {

    override fun selectDeserializer(element: JsonElement) = when {
        "tps" in element.jsonObject -> TpsRate.serializer()
        "period" in element.jsonObject -> PeriodRate.serializer()
        else -> error("cannot deserialize $element")
    }
}