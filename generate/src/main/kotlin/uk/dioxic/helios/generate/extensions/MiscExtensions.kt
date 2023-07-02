package uk.dioxic.helios.generate.extensions

import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import uk.dioxic.helios.generate.ejson.BsonDecoder
import uk.dioxic.helios.generate.ejson.BsonEncoder
import java.util.*

val myLocale: Locale = Locale.ENGLISH

fun Iterable<Number>.average(): Double {
    var sum: Double = 0.0
    var count: Int = 0
    for (element in this) {
        sum += element.toDouble()
        count++
        if (count < 0) {
            throw ArithmeticException("Count overflow has happened.")
        }
    }
    return if (count == 0) Double.NaN else sum / count
}

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
                "Expected Decoder to be BsonDecoder, got ${this::class}"
    )

internal fun Encoder.asBsonEncoder() = this as? BsonEncoder
    ?: throw IllegalStateException(
        "This serializer can be used only with Bson format." +
                "Expected Encoder to be BsonEncoder, got ${this::class}"
    )