package uk.dioxic.helios.generate.operators

import uk.dioxic.helios.generate.Operator
import uk.dioxic.helios.generate.OperatorContext
import uk.dioxic.helios.generate.Wrapped
import uk.dioxic.helios.generate.annotations.Alias

@Alias("root")
class RootOperator(val value: Wrapped<Any?>) : Operator<Any?> {

    context(OperatorContext)
    override fun invoke(): Any? =
        value.invoke()
}