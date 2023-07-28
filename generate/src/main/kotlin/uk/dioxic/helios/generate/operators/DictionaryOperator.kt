package uk.dioxic.helios.generate.operators

import uk.dioxic.helios.generate.OperatorContext
import uk.dioxic.helios.generate.annotations.Alias

@Alias("dict")
class DictionaryOperator(override val key: String) : KeyedOperator<Any>() {

    context(OperatorContext)
    override fun invoke(): Any = dictionaries[key] ?: error("dictionary [$key] not found")
}