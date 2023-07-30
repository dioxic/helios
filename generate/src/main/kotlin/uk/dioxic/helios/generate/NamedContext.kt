package uk.dioxic.helios.generate

data class NamedContext(
    val name: String,
    override val variables: Map<String, Any?> = emptyMap(),
    override val dictionaries: Map<String, Any?> = emptyMap(),
    override val count: Long = -1L
) : OperatorContext