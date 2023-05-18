package uk.dioxic.mgenerate.operators.general

import uk.dioxic.mgenerate.annotations.Alias
import uk.dioxic.mgenerate.operators.Operator

@Alias("array")
class ArrayOperator(
    val of: () -> Any,
    val number: () -> Int = { 5 }
) : Operator<Any> {

    override fun invoke() =
        List(number()) {
            of()
        }
}