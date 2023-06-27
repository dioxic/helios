package uk.dioxic.helios.execute.serialization

import com.mongodb.TransactionOptions
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.concurrent.TimeUnit

object TransactionOptionsSerializer : KSerializer<TransactionOptions> {
    override val descriptor: SerialDescriptor =
        TransactionOptionsSurrogate.serializer().descriptor


    override fun deserialize(decoder: Decoder): TransactionOptions {
        val surrogate = decoder.decodeSerializableValue(TransactionOptionsSurrogate.serializer())
        return TransactionOptions.builder()
            .readPreference(surrogate.readPreference)
            .writeConcern(surrogate.writeConcern)
            .readConcern(surrogate.readConcern)
            .maxCommitTime(surrogate.maxCommitTimeMS, TimeUnit.MILLISECONDS)
            .build()
    }

    override fun serialize(encoder: Encoder, value: TransactionOptions) {
        encoder.encodeSerializableValue(
            TransactionOptionsSurrogate.serializer(), TransactionOptionsSurrogate(
                readPreference = value.readPreference,
                writeConcern = value.writeConcern,
                readConcern = value.readConcern,
                maxCommitTimeMS = value.getMaxCommitTime(TimeUnit.MILLISECONDS)
            )
        )
    }
}