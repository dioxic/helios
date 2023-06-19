package uk.dioxic.mgenerate.worker.model

import arrow.optics.optics
import uk.dioxic.mgenerate.Template
import uk.dioxic.mgenerate.operators.Operator

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

@optics
data class ExecutionState(
    val custom: Map<String, Any?>,
    val executionCount: Long,
    val startTimeMillis: Long,
) {
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

val Workload.hydratedState
    get() = State(state.hydrate())

val Stage.hydratedState
    get() = State(state.hydrate())

val Benchmark.hydratedState
    get() = State(state.hydrate())