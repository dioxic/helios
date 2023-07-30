package uk.dioxic.helios.generate

interface OperatorContext {
    val dictionaries: Map<String, Any?>
    val variables: Map<String, Any?>
    val count: Long

    companion object {
        val EMPTY: OperatorContext = NamedContext("EMPTY")

        val threadLocal: ThreadLocal<OperatorContext> =
            ThreadLocal.withInitial { EMPTY }
    }

}