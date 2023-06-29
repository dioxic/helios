package uk.dioxic.helios.execute.model

import uk.dioxic.helios.execute.format.flatten

class State private constructor(map: Map<String, Any?>) {

    private val state = map.flatten('.')

    operator fun get(key: String) = state[key]

    operator fun plus(other: State) =
        State(buildMap {
            putAll(state)
            putAll(other.state)
        })

    companion object {
        val EMPTY = State(emptyMap())

        operator fun invoke(map: Map<String, Any?> = emptyMap()) =
            State(map.flatten('.'))

    }
}