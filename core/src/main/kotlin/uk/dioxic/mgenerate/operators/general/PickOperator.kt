package uk.dioxic.mgenerate.operators.general

import uk.dioxic.mgenerate.annotations.Alias
import uk.dioxic.mgenerate.operators.Operator

@Alias("pick")
class PickOperator(
    val array: () -> List<Any>,
    val element: () -> Int = { 0 },
) : Operator<Any> {
    override fun invoke(): Any =
        array()[element()]
}