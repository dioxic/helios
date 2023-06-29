package uk.dioxic.helios.execute

import uk.dioxic.helios.generate.KeyedOperator
import uk.dioxic.helios.generate.annotations.Alias

@Alias("state")
class StateOperator(override val key: String) : KeyedOperator<Any?>() {
    override fun invoke(): Any? =
        StateManager[key]
}