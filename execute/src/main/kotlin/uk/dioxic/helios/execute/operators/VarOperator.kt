package uk.dioxic.helios.execute.operators

import uk.dioxic.helios.generate.KeyedOperator
import uk.dioxic.helios.generate.OperatorContext
import uk.dioxic.helios.generate.annotations.Alias

@Alias("var")
class VarOperator(override val key: String) : KeyedOperator<Any?>() {

    context(OperatorContext)
    override fun invoke(): Any? = variables.value[key]
}