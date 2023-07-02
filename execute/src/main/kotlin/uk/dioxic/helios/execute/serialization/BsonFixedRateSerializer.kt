package uk.dioxic.helios.execute.serialization

import org.bson.BsonValue
import uk.dioxic.helios.execute.model.FixedRate
import uk.dioxic.helios.execute.model.PeriodRate
import uk.dioxic.helios.execute.model.TpsRate

object BsonFixedRateSerializer : BsonContentPolymorphicSerializer<FixedRate>(FixedRate::class) {

    override fun selectDeserializer(element: BsonValue) = when {
        "tps" in element.asDocument() -> TpsRate.serializer()
        "period" in element.asDocument() -> PeriodRate.serializer()
        else -> error("cannot deserialize $element")
    }
}