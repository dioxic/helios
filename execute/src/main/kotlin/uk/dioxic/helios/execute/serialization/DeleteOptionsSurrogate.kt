package uk.dioxic.helios.execute.serialization

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.BsonDocument

@Serializable
data class DeleteOptionsSurrogate(
    @Contextual val hint: BsonDocument? = null,
    val hintString: String? = null,
    val comment: String? = null,
)