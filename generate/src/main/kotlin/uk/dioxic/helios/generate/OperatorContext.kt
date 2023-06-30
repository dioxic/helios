package uk.dioxic.helios.generate

interface OperatorContext {
    val identity: Named
    val constants: Lazy<Map<String, Any?>>
    val variables: Lazy<Map<String, Any?>>
    val executionCount: Long

    companion object {
        val EMPTY = object : OperatorContext {
            override val identity = object : Named {
                override val name = "EMPTY"
            }
            override val constants = lazy { emptyMap<String, Any?>() }
            override val variables = lazy { emptyMap<String, Any?>() }
            override val executionCount = 0L
        }
    }
}