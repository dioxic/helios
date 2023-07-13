package uk.dioxic.helios.execute.serialization

import kotlinx.serialization.bson.BsonContentPolymorphicSerializer
import org.bson.BsonDocument
import uk.dioxic.helios.execute.model.RateWorkload
import uk.dioxic.helios.execute.model.WeightedWorkload
import uk.dioxic.helios.execute.model.Workload

object WorkloadSerializer : BsonContentPolymorphicSerializer<Workload>(Workload::class) {

    override fun selectDeserializer(document: BsonDocument) = when {
        "weight" in document.asDocument() -> WeightedWorkload.serializer()
        else -> RateWorkload.serializer()
    }
}