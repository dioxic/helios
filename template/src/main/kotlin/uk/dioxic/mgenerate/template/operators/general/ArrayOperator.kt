package uk.dioxic.mgenerate.template.operators.general

import uk.dioxic.mgenerate.template.operators.Operator
import kotlin.math.max

@uk.dioxic.mgenerate.template.annotations.Alias("array")
class ArrayOperator(
    val of: () -> Any,
    val number: () -> Int = { 5 }
) : Operator<Any> {

    override fun invoke() =
        List(max(number(), 0)) {
            of()
        }
}