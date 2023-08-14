package uk.dioxic.helios.execute.serialization

import kotlinx.serialization.Serializable

@Serializable
data class BulkWriteOptionsSurrogate(
    val ordered: Boolean = true,
    val bypassDocumentValidation: Boolean? = null,
    val comment: String? = null,
)