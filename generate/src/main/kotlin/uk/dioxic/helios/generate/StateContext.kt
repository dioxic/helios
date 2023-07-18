package uk.dioxic.helios.generate

import arrow.optics.optics

@optics
data class StateContext(
    override val count: Long = -1,
    override val constants: Lazy<Map<String, Any?>> = lazy { emptyMap() },
    override val variables: Lazy<Map<String, Any?>> = lazy { emptyMap() },
) : OperatorContext {

    override fun withConstants(constants: Lazy<Map<String, Any?>>) =
        copy(constants = constants)

    override fun withVariables(variables: Lazy<Map<String, Any?>>) =
        copy(variables = variables)

    companion object
}
