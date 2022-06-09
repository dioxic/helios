package uk.dioxic.mgenerate.ksp.model

data class DefaultValue(
    val code: String,
    val imports: List<String> = emptyList()
)