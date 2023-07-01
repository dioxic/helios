package uk.dioxic.helios.generate.operators

import uk.dioxic.helios.generate.Operator
import uk.dioxic.helios.generate.OperatorContext
import uk.dioxic.helios.generate.Wrapped
import uk.dioxic.helios.generate.annotations.Alias

@Alias("root")
class RootOperator(val value: Wrapped<Map<String,Any?>>) : Operator<Map<String,Any?>> {

    context(OperatorContext)
    override fun invoke(): Map<String,Any?> =
        value.invoke()
}