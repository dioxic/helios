package uk.dioxic.mgenerate.execute

import org.apache.commons.math3.stat.StatUtils
import uk.dioxic.mgenerate.execute.results.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun List<Duration>.summarize() = SummarizedLatencies(
    p50 = percentile(0.5),
    p95 = percentile(0.95),
    p99 = percentile(0.95),
    max = max(),
    min = min(),
)

fun Iterable<Duration>.percentile(percentile: Double) =
    map { it.toDouble(DurationUnit.MILLISECONDS) }.percentile(percentile)

fun Sequence<Duration>.percentile(percentile: Double) =
    map { it.toDouble(DurationUnit.MILLISECONDS) }
        .toList().percentile(percentile)

fun List<Double>.percentile(percentile: Double) =
    StatUtils.percentile(toDoubleArray(), percentile)
        .toDuration(DurationUnit.MILLISECONDS)


fun List<TimedWriteResult>.summarize(workloadName: String) =
    SummarizedWriteResult(
        workloadName = workloadName,
        insertCount = sumOf { it.value.insertCount },
        matchedCount = sumOf { it.value.matchedCount },
        modifiedCount = sumOf { it.value.modifiedCount },
        deletedCount = sumOf { it.value.deletedCount },
        upsertedCount = sumOf { it.value.upsertedCount },
        latencies = map { it.duration }.summarize()
    )

fun List<TimedReadResult>.summarize(workloadName: String) =
    SummarizedReadResult(
        workloadName = workloadName,
        docReturned = sumOf { it.value.docReturned },
        queryCount = size,
        latencies = map { it.duration }.summarize()
    )

fun List<TimedMessageResult>.summarize(workloadName: String) =
    SummarizedMessageResult(
        workloadName = workloadName,
        msgCount = size,
        latencies = map { it.duration }.summarize()
    )

fun List<TimedCommandResult>.summarize(workloadName: String) =
    SummarizedCommandResult(
        workloadName = workloadName,
        successes = count { it.value.success },
        failures = count { !it.value.success },
        latencies = map { it.duration }.summarize()
    )

@Suppress("UNCHECKED_CAST")
fun List<TimedResult>.summarize(workloadName: String): SummarizedResult =
    when (first()) {
        is TimedWriteResult -> (this as List<TimedWriteResult>).summarize(workloadName)
        is TimedReadResult -> (this as List<TimedReadResult>).summarize(workloadName)
        is TimedMessageResult -> (this as List<TimedMessageResult>).summarize(workloadName)
        is TimedCommandResult -> (this as List<TimedCommandResult>).summarize(workloadName)
    }