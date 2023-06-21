package uk.dioxic.mgenerate.template.operators.general

import uk.dioxic.mgenerate.template.operators.Operator

class OptionalOperator(val value: () -> Any) : Operator<Any> {
    override fun invoke(): Any = value()
}