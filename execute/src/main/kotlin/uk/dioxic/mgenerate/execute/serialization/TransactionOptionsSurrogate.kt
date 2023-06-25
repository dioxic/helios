package uk.dioxic.mgenerate.execute.serialization

import com.mongodb.ReadConcern
import com.mongodb.ReadPreference
import com.mongodb.WriteConcern
import kotlinx.serialization.Serializable

@Serializable
data class TransactionOptionsSurrogate(
    @Serializable(ReadConcernSerializer::class) val readConcern: ReadConcern? = null,
    @Serializable(WriteConcernSerializer::class) val writeConcern: WriteConcern? = null,
    @Serializable(ReadPreferenceSerializer::class) val readPreference: ReadPreference? = null,
    val maxCommitTimeMS: Long? = null
)