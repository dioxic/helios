package uk.dioxic.mgenerate.worker.model

import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

object WorkloadSerializer : JsonContentPolymorphicSerializer<Workload>(Workload::class) {

    override fun selectDeserializer(element: JsonElement) = when {
        "weight" in element.jsonObject -> WeightedWorkload.serializer()
        else -> RateWorkload.serializer()
    }
}