package uk.dioxic.mgenerate.execute.results

import org.apache.commons.math3.stat.StatUtils
import uk.dioxic.mgenerate.execute.model.ExecutionContext
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun List<Duration>.summarize() = SummarizedLatencies(
    p50 = percentile(0.5),
    p95 = percentile(0.95),
    p99 = percentile(0.99),
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


fun List<TimedWriteResult>.summarize(context: ExecutionContext) =
    SummarizedWriteResult(
        context = context,
        insertCount = sumOf { it.value.insertCount },
        matchedCount = sumOf { it.value.matchedCount },
        modifiedCount = sumOf { it.value.modifiedCount },
        deletedCount = sumOf { it.value.deletedCount },
        upsertedCount = sumOf { it.value.upsertedCount },
        latencies = map { it.duration }.summarize()
    )

fun List<TimedReadResult>.summarize(context: ExecutionContext) =
    SummarizedReadResult(
        context = context,
        docReturned = sumOf { it.value.docReturned },
        queryCount = size,
        latencies = map { it.duration }.summarize()
    )

fun List<TimedMessageResult>.summarize(context: ExecutionContext) =
    SummarizedMessageResult(
        context = context,
        msgCount = size,
        latencies = map { it.duration }.summarize()
    )

fun List<TimedCommandResult>.summarize(context: ExecutionContext) =
    SummarizedCommandResult(
        context = context,
        successes = count { it.value.success },
        failures = count { !it.value.success },
        latencies = map { it.duration }.summarize()
    )

@Suppress("UNCHECKED_CAST")
fun List<TimedResult>.summarize() =
    groupBy(TimedResult::context)
        .map { (k, v) ->
            when (v.first()) {
                is TimedWriteResult -> (this as List<TimedWriteResult>).summarize(k)
                is TimedReadResult -> (this as List<TimedReadResult>).summarize(k)
                is TimedMessageResult -> (this as List<TimedMessageResult>).summarize(k)
                is TimedCommandResult -> (this as List<TimedCommandResult>).summarize(k)
            }
        }
        .sortedBy { it.context.workload.name }