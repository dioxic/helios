package uk.dioxic.helios.execute.fixtures

import com.mongodb.ReadConcern
import com.mongodb.ReadPreference
import kotlinx.serialization.Serializable
import uk.dioxic.helios.execute.serialization.ReadConcernSerializer
import uk.dioxic.helios.execute.serialization.ReadPreferenceSerializer

@Serializable
data class DataClassWithReadConcern (
    @Serializable(with = ReadConcernSerializer::class) val readConcern: ReadConcern
)

@Serializable
data class DataClassWithReadPreference (
    @Serializable(with = ReadPreferenceSerializer::class) val readPreference: ReadPreference
)