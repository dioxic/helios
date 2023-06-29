package uk.dioxic.helios.execute

import uk.dioxic.helios.execute.model.State

object StateManager {

    private var state: State = State.EMPTY

    fun setState(state: State) {
        this.state = state
    }

    fun clearState() {
        state = State.EMPTY
    }

    operator fun get(key: String) = state[key]

}