package uk.dioxic.helios.execute.serialization

import org.bson.BsonValue
import uk.dioxic.helios.execute.model.RateWorkload
import uk.dioxic.helios.execute.model.WeightedWorkload
import uk.dioxic.helios.execute.model.Workload

object BsonWorkloadSerializer : BsonContentPolymorphicSerializer<Workload>(Workload::class) {

    override fun selectDeserializer(element: BsonValue) = when {
        "weight" in element.asDocument() -> WeightedWorkload.serializer()
        else -> RateWorkload.serializer()
    }
}