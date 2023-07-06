package uk.dioxic.helios.execute.serialization

import kotlinx.serialization.bson.BsonContentPolymorphicSerializer
import org.bson.BsonDocument
import uk.dioxic.helios.execute.model.FixedRate
import uk.dioxic.helios.execute.model.PeriodRate
import uk.dioxic.helios.execute.model.TpsRate

object FixedRateSerializer : BsonContentPolymorphicSerializer<FixedRate>(FixedRate::class) {

    override fun selectDeserializer(element: BsonDocument) = when {
        "tps" in element.asDocument() -> TpsRate.serializer()
        "period" in element.asDocument() -> PeriodRate.serializer()
        else -> error("cannot deserialize $element")
    }
}