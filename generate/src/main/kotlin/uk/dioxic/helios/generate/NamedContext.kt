package uk.dioxic.helios.generate

class NamedContext(named: Named) : OperatorContext {
    override val identity = named
    override val constants = lazy { emptyMap<String, Any?>() }
    override val executionCount = -1L
}