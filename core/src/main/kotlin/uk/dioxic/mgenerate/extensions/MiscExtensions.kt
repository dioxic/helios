package uk.dioxic.mgenerate.extensions

import uk.dioxic.mgenerate.worker.Named
import uk.dioxic.mgenerate.worker.Rate
import uk.dioxic.mgenerate.worker.results.*
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

val myLocale: Locale = Locale.ENGLISH

inline val Int.tps get() = Rate.of(this)

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
inline fun Named.measureTimedResult(block: () -> Result): TimedResult {
    val mark = TimeSource.Monotonic.markNow()
    return when (val value = block()) {
        is WriteResult -> TimedWriteResult(value, mark.elapsedNow(), name)
        is ReadResult -> TimedReadResult(value, mark.elapsedNow(), name)
        is MessageResult -> TimedMessageResult(value, mark.elapsedNow(), name)
        is CommandResult -> TimedCommandResult(value, mark.elapsedNow(), name)
    }
}

@OptIn(ExperimentalTime::class)
inline fun measureTimedResult(name: String, block: () -> Result): TimedResult {
    val mark = TimeSource.Monotonic.markNow()
    return when (val value = block()) {
        is WriteResult -> TimedWriteResult(value, mark.elapsedNow(), name)
        is ReadResult -> TimedReadResult(value, mark.elapsedNow(), name)
        is MessageResult -> TimedMessageResult(value, mark.elapsedNow(), name)
        is CommandResult -> TimedCommandResult(value, mark.elapsedNow(), name)
    }
}