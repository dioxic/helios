package uk.dioxic.mgenerate.operators.general

import org.bson.types.ObjectId
import uk.dioxic.mgenerate.annotations.Alias
import uk.dioxic.mgenerate.operators.Operator

@Alias("objectId")
class ObjectIdOperator : Operator<ObjectId> {
    override fun invoke(): ObjectId =
        ObjectId.get()
}