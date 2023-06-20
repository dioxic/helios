package uk.dioxic.mgenerate.worker.model

import arrow.optics.optics
import uk.dioxic.mgenerate.Template
import uk.dioxic.mgenerate.operators.Operator
import uk.dioxic.mgenerate.worker.Stateful

@optics
data class State(
    val custom: Map<String, Any?>
) {
    operator fun plus(other: State) =
        other.copy(custom = buildMap {
            putAll(custom)
            putAll(other.custom)
        })

    companion object
}

fun Template.hydrate() = hydrateMap(this)

@Suppress("UNCHECKED_CAST")
private fun hydrateMap(map: Map<String, Any?>): Map<String, Any?> =
    map.mapValues { (_, v) ->
        when (v) {
            is Operator<*> -> v.invoke()
            is Map<*, *> -> hydrateMap(v as Map<String, Any?>)
            else -> v
        }
    }

fun combine(vararg stateful: Stateful) {

}