package uk.dioxic.mgenerate.execute.results

import org.apache.commons.math3.stat.StatUtils
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun List<Duration>.summarize() = SummarizedLatencies(
    p50 = percentile(0.5),
    p95 = percentile(0.95),
    p99 = percentile(0.99),
    max = max(),
)

fun Iterable<Duration>.percentile(percentile: Double) =
    map { it.toDouble(DurationUnit.MILLISECONDS) }.percentile(percentile)

fun Sequence<Duration>.percentile(percentile: Double) =
    map { it.toDouble(DurationUnit.MILLISECONDS) }
        .toList().percentile(percentile)

fun List<Double>.percentile(percentile: Double) =
    StatUtils.percentile(toDoubleArray(), percentile)
        .toDuration(DurationUnit.MILLISECONDS)

fun List<TimedResult>.summarize() =
    groupBy { it.context.workload.name }
        .map { (_, v) ->
            val acc = v.fold(ResultAccumulator()) { acc, r -> acc.add(r) }

            when (v.first()) {
                is TimedWriteResult -> SummarizedWriteResult(acc)
                is TimedReadResult -> SummarizedReadResult(acc)
                is TimedMessageResult -> SummarizedMessageResult(acc)
                is TimedCommandResult -> SummarizedCommandResult(acc)
                is TimedTransactionResult -> SummarizedTransactionResult(acc)
                is TimedErrorResult -> SummarizedErrorResult(acc)
            }
        }.sortedBy { it.context.workload.name }

operator fun SummarizedMessageResult.Companion.invoke(accumulator: ResultAccumulator) =
    SummarizedMessageResult(
        context = accumulator.context!!,
        latencies = accumulator.durations.summarize(),
        elapsedTime = accumulator.elapsedTime,
        operationCount = accumulator.operationCount,
    )

operator fun SummarizedWriteResult.Companion.invoke(accumulator: ResultAccumulator) =
    SummarizedWriteResult(
        context = accumulator.context!!,
        insertedCount = accumulator.insertedCount,
        matchedCount = accumulator.matchedCount,
        modifiedCount = accumulator.modifiedCount,
        deletedCount = accumulator.deletedCount,
        upsertedCount = accumulator.upsertedCount,
        latencies = accumulator.durations.summarize(),
        elapsedTime = accumulator.elapsedTime,
        operationCount = accumulator.operationCount,
    )

operator fun SummarizedReadResult.Companion.invoke(accumulator: ResultAccumulator) =
    SummarizedReadResult(
        context = accumulator.context!!,
        docsReturned = accumulator.docsReturned,
        latencies = accumulator.durations.summarize(),
        elapsedTime = accumulator.elapsedTime,
        operationCount = accumulator.operationCount,
    )

operator fun SummarizedCommandResult.Companion.invoke(accumulator: ResultAccumulator) =
    SummarizedCommandResult(
        context = accumulator.context!!,
        successCount = accumulator.successCount,
        failureCount = accumulator.failureCount,
        latencies = accumulator.durations.summarize(),
        elapsedTime = accumulator.elapsedTime,
        operationCount = accumulator.operationCount,
    )

operator fun SummarizedErrorResult.Companion.invoke(accumulator: ResultAccumulator) =
    SummarizedErrorResult(
        context = accumulator.context!!,
        errorCount = accumulator.errors.size,
        distinctErrors = accumulator.errors.distinctBy { it::class to it.message },
        latencies = accumulator.durations.summarize(),
        elapsedTime = accumulator.elapsedTime,
        operationCount = accumulator.operationCount,
    )

operator fun SummarizedTransactionResult.Companion.invoke(accumulator: ResultAccumulator) =
    SummarizedTransactionResult(
        context = accumulator.context!!,
        insertedCount = accumulator.insertedCount,
        matchedCount = accumulator.matchedCount,
        modifiedCount = accumulator.modifiedCount,
        deletedCount = accumulator.deletedCount,
        upsertedCount = accumulator.upsertedCount,
        docsReturned = accumulator.docsReturned,
        successCount = accumulator.successCount,
        failureCount = accumulator.failureCount,
        latencies = accumulator.durations.summarize(),
        elapsedTime = accumulator.elapsedTime,
        operationCount = accumulator.operationCount,
    )