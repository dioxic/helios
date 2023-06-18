package uk.dioxic.mgenerate.operators.general

import uk.dioxic.mgenerate.operators.Operator

class OptionalOperator(val value: () -> Any) : Operator<Any> {
    override fun invoke(): Any = value()
}