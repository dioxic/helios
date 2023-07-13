package uk.dioxic.helios.execute.serialization

import kotlinx.serialization.bson.BsonContentPolymorphicSerializer
import org.bson.BsonDocument
import uk.dioxic.helios.execute.model.PeriodRate
import uk.dioxic.helios.execute.model.RampedRate
import uk.dioxic.helios.execute.model.Rate
import uk.dioxic.helios.execute.model.TpsRate

object RateSerializer : BsonContentPolymorphicSerializer<Rate>(Rate::class) {

    override fun selectDeserializer(document: BsonDocument) = when {
        "period" in document.asDocument() -> PeriodRate.serializer()
        "rampDuration" in document.asDocument() -> RampedRate.serializer()
        else -> TpsRate.serializer()
    }
}