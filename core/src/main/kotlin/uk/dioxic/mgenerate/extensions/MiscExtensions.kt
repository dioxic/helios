package uk.dioxic.mgenerate.extensions

import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import uk.dioxic.mgenerate.worker.model.Workload
import uk.dioxic.mgenerate.worker.results.*
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

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

@OptIn(ExperimentalTime::class)
inline fun measureTimedResult(workload: Workload, block: () -> Result): TimedResult {
    val mark = TimeSource.Monotonic.markNow()
    return when (val value = block()) {
        is WriteResult -> TimedWriteResult(value, mark.elapsedNow(), workload)
        is ReadResult -> TimedReadResult(value, mark.elapsedNow(), workload)
        is MessageResult -> TimedMessageResult(value, mark.elapsedNow(), workload)
        is CommandResult -> TimedCommandResult(value, mark.elapsedNow(), workload)
    }
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