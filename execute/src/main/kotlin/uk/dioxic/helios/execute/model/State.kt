package uk.dioxic.helios.execute.model

import arrow.optics.optics

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