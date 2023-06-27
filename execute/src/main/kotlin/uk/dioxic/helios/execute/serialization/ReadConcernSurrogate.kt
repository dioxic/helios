package uk.dioxic.helios.execute.serialization

import com.mongodb.ReadConcernLevel
import kotlinx.serialization.Serializable

@Serializable
data class ReadConcernSurrogate(val level: ReadConcernLevel)