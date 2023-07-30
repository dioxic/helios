package uk.dioxic.helios.generate

import arrow.optics.optics

@optics
data class StateContext(
    override val count: Long = -1,
    override val dictionaries: Map<String, Any?> = emptyMap(),
    override val variables: Map<String, Any?> = emptyMap(),
) : OperatorContext
