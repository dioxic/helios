package uk.dioxic.helios.execute.serialization

import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import uk.dioxic.helios.execute.model.RateWorkload
import uk.dioxic.helios.execute.model.WeightedWorkload
import uk.dioxic.helios.execute.model.Workload

object WorkloadSerializer : JsonContentPolymorphicSerializer<Workload>(Workload::class) {

    override fun selectDeserializer(element: JsonElement) = when {
        "weight" in element.jsonObject -> WeightedWorkload.serializer()
        else -> RateWorkload.serializer()
    }
}