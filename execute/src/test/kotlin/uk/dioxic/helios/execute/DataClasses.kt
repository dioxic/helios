package uk.dioxic.helios.execute

import com.mongodb.ReadConcern
import com.mongodb.ReadPreference
import kotlinx.serialization.Serializable
import uk.dioxic.helios.execute.model.Store
import uk.dioxic.helios.execute.serialization.ReadConcernSerializer
import uk.dioxic.helios.execute.serialization.ReadPreferenceSerializer

@Serializable
data class DataClassWithReadConcern(
    @Serializable(with = ReadConcernSerializer::class) val readConcern: ReadConcern
)

@Serializable
data class DataClassWithReadPreference(
    @Serializable(with = ReadPreferenceSerializer::class) val readPreference: ReadPreference
)

@Serializable
data class DataClassWithStore(
    val store: Store
)