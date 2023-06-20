package uk.dioxic.mgenerate.template.operators.general

import org.bson.types.ObjectId
import uk.dioxic.mgenerate.template.annotations.Alias
import uk.dioxic.mgenerate.template.operators.Operator

@Alias("objectId")
class ObjectIdOperator : Operator<ObjectId> {
    override fun invoke(): ObjectId =
        ObjectId.get()
}