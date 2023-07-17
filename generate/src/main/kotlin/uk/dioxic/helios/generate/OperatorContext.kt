package uk.dioxic.helios.generate

interface OperatorContext {
    val constants: Lazy<Map<String, Any?>>
    val variables: Lazy<Map<String, Any?>>
    val executionCount: Long

    fun withConstants(constants: Lazy<Map<String, Any?>>): OperatorContext

    fun withVariables(variables: Lazy<Map<String, Any?>>): OperatorContext

    companion object {
        val EMPTY: OperatorContext = NamedContext("EMPTY")

        val threadLocal: ThreadLocal<OperatorContext> =
            ThreadLocal.withInitial { EMPTY }
    }

}