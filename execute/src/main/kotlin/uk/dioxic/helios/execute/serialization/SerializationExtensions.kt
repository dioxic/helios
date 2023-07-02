package uk.dioxic.helios.execute.serialization

import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import uk.dioxic.helios.generate.ejson.BsonDecoder
import uk.dioxic.helios.generate.ejson.BsonEncoder

internal fun Decoder.asJsonDecoder(): JsonDecoder = this as? JsonDecoder
    ?: throw IllegalStateException(
        "This serializer can be used only with Json format." +
                "Expected Decoder to be JsonDecoder, got ${this::class}"
    )

internal fun Encoder.asJsonEncoder() = this as? JsonEncoder
    ?: throw IllegalStateException(
        "This serializer can be used only with Json format." +
                "Expected Encoder to be JsonEncoder, got ${this::class}"
    )

internal fun Decoder.asBsonDecoder(): BsonDecoder = this as? BsonDecoder
    ?: throw IllegalStateException(
        "This serializer can be used only with Bson format." +
                "Expected Decoder to be uk.dioxic.helios.execute.ejson.BsonDecoder, got ${this::class}"
    )

internal fun Encoder.asBsonEncoder() = this as? BsonEncoder
    ?: throw IllegalStateException(
        "This serializer can be used only with Bson format." +
                "Expected Encoder to be BsonEncoder, got ${this::class}"
    )