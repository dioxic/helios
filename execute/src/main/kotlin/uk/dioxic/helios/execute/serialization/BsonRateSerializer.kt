package uk.dioxic.helios.execute.serialization

import org.bson.BsonValue
import uk.dioxic.helios.execute.model.PeriodRate
import uk.dioxic.helios.execute.model.RampedRate
import uk.dioxic.helios.execute.model.Rate
import uk.dioxic.helios.execute.model.TpsRate

object BsonRateSerializer : BsonContentPolymorphicSerializer<Rate>(Rate::class) {

    override fun selectDeserializer(element: BsonValue) = when {
        "period" in element.asDocument() -> PeriodRate.serializer()
        "rampDuration" in element.asDocument() -> RampedRate.serializer()
        else -> TpsRate.serializer()
    }
}