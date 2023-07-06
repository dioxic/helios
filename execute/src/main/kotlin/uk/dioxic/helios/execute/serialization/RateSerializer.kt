package uk.dioxic.helios.execute.serialization

import kotlinx.serialization.bson.BsonContentPolymorphicSerializer
import org.bson.BsonDocument
import uk.dioxic.helios.execute.model.PeriodRate
import uk.dioxic.helios.execute.model.RampedRate
import uk.dioxic.helios.execute.model.Rate
import uk.dioxic.helios.execute.model.TpsRate

object RateSerializer : BsonContentPolymorphicSerializer<Rate>(Rate::class) {

    override fun selectDeserializer(element: BsonDocument) = when {
        "period" in element.asDocument() -> PeriodRate.serializer()
        "rampDuration" in element.asDocument() -> RampedRate.serializer()
        else -> TpsRate.serializer()
    }
}