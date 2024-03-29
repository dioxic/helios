package uk.dioxic.helios.generate

data class NamedContext(
    val name: String,
    override val constants: Lazy<Map<String, Any?>> = lazy { emptyMap() },
    override val variables: Lazy<Map<String, Any?>> = lazy { emptyMap() },
    override val count: Long = -1L
) : OperatorContext {

    override fun withConstants(constants: Lazy<Map<String, Any?>>) =
        this.copy(constants = constants)

    override fun withVariables(variables: Lazy<Map<String, Any?>>) =
        this.copy(variables = variables)
}