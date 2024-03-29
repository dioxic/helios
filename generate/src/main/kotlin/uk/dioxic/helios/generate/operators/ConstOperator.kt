package uk.dioxic.helios.generate.operators

import uk.dioxic.helios.generate.OperatorContext
import uk.dioxic.helios.generate.annotations.Alias

@Alias("const")
class ConstOperator(override val key: String) : KeyedOperator<Any>() {

    context(OperatorContext)
    override fun invoke(): Any = constants.value[key] ?: error("constant [$key] not found")
}