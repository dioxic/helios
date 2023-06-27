package uk.dioxic.helios.generate.operators.general

import uk.dioxic.helios.generate.annotations.Alias
import uk.dioxic.helios.generate.operators.Operator
import kotlin.math.max

@Alias("array")
class ArrayOperator(
    val of: () -> Any,
    val number: () -> Int = { 5 }
) : Operator<Any> {

    override fun invoke() =
        List(max(number(), 0)) {
            of()
        }
}