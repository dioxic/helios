package uk.dioxic.helios.generate.operators.general

import org.bson.types.ObjectId
import uk.dioxic.helios.generate.annotations.Alias
import uk.dioxic.helios.generate.operators.Operator

@Alias("objectId")
class ObjectIdOperator : Operator<ObjectId> {
    override fun invoke(): ObjectId =
        ObjectId.get()
}