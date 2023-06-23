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

fun List<WriteResult>.sumOf() = WriteResult(
    insertedCount = sumOf { it.insertedCount },
    matchedCount = sumOf { it.matchedCount },
    modifiedCount = sumOf { it.modifiedCount },
    deletedCount = sumOf { it.deletedCount },
    upsertedCount = sumOf { it.upsertedCount },
)

private fun List<TimedWriteResult>.summarize(context: ExecutionContext) =
    SummarizedWriteResult(
        context = context,
        insertedCount = sumOf { it.value.insertedCount },
        matchedCount = sumOf { it.value.matchedCount },
        modifiedCount = sumOf { it.value.modifiedCount },
        deletedCount = sumOf { it.value.deletedCount },
        upsertedCount = sumOf { it.value.upsertedCount },
        latencies = map { it.duration }.summarize(),
        elapsedTime = maxOf { it.elapsedTime },
    )

private fun List<TimedReadResult>.summarize(context: ExecutionContext) =
    SummarizedReadResult(
        context = context,
        docsReturned = sumOf { it.value.docReturned },
        operationCount = size,
        latencies = map { it.duration }.summarize(),
        elapsedTime = maxOf { it.elapsedTime },
    )

private fun List<TimedMessageResult>.summarize(context: ExecutionContext) =
    SummarizedMessageResult(
        context = context,
        msgCount = size,
        latencies = map { it.duration }.summarize(),
        elapsedTime = maxOf { it.elapsedTime },
    )

private fun List<TimedCommandResult>.summarize(context: ExecutionContext) =
    SummarizedCommandResult(
        context = context,
        successes = count { it.value.success },
        failures = count { !it.value.success },
        latencies = map { it.duration }.summarize(),
        elapsedTime = maxOf { it.elapsedTime },
    )

@Suppress("UNCHECKED_CAST")
fun List<TimedResult>.summarize() =
    groupBy { it.context.workload.name }
        .map { (_, v) ->
            val context = v.last().context
            when (v.first()) {
                is TimedWriteResult -> (v as List<TimedWriteResult>).summarize(context)
                is TimedReadResult -> (v as List<TimedReadResult>).summarize(context)
                is TimedMessageResult -> (v as List<TimedMessageResult>).summarize(context)
                is TimedCommandResult -> (v as List<TimedCommandResult>).summarize(context)
            }
        }
        .sortedBy { it.context.workload.name }