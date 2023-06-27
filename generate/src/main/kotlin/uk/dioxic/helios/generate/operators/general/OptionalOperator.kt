package uk.dioxic.helios.generate.operators.general

import uk.dioxic.helios.generate.operators.Operator

class OptionalOperator(val value: () -> Any) : Operator<Any> {
    override fun invoke(): Any = value()
}