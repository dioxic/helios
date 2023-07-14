package uk.dioxic.helios.execute.serialization

import kotlinx.serialization.Serializable

@Serializable
data class InsertOneOptionsSurrogate(
    val bypassDocumentValidation: Boolean? = null,
    val comment: String? = null,
)