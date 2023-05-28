package uk.dioxic.mgenerate.operators.general

import org.bson.types.ObjectId
import uk.dioxic.mgenerate.annotations.Alias
import uk.dioxic.mgenerate.annotations.Operator

@Alias("objectId")
class ObjectIdOperator : Operator<ObjectId> {
    override fun invoke(): ObjectId =
        ObjectId.get()
}