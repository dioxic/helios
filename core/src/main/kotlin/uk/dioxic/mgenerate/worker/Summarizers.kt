package uk.dioxic.mgenerate.worker

import uk.dioxic.mgenerate.utils.percentile
import uk.dioxic.mgenerate.worker.results.*


fun List<TimedWriteResult>.summarize(workloadName: String) =
    SummarizedWriteResult(
        workloadName = workloadName,
        insertCount = sumOf { it.value.insertCount },
        matchedCount = sumOf { it.value.matchedCount },
        modifiedCount = sumOf { it.value.modifiedCount },
        deletedCount = sumOf { it.value.deletedCount },
        upsertedCount = sumOf { it.value.upsertedCount },
        latencyPercentiles = listOf(
            "p50" to map { it.duration }.percentile(0.5),
            "p95" to map { it.duration }.percentile(0.95),
            "p99" to map { it.duration }.percentile(0.99)
        )
    )

fun List<TimedReadResult>.summarize(workloadName: String) =
    SummarizedReadResult(
        workloadName = workloadName,
        docReturned = sumOf { it.value.docReturned },
        queryCount = size,
        latencyPercentiles = listOf(
            "p50" to map { it.duration }.percentile(0.5)
        )
    )

fun List<TimedMessageResult>.summarize(workloadName: String) =
    SummarizedMessageResult(
        workloadName = workloadName,
        msgCount = size,
        latencyPercentiles = listOf(
            "p50" to map { it.duration }.percentile(0.5)
        )
    )

fun List<TimedCommandResult>.summarize(workloadName: String) =
    SummarizedCommandResult(
        workloadName = workloadName,
        successes = count { it.value.success },
        failures = count { !it.value.success },
        latencyPercentiles = listOf(
            "p50" to map { it.duration }.percentile(0.5)
        )
    )

@Suppress("UNCHECKED_CAST")
fun List<TimedResult>.summarize(workloadName: String): SummarizedResult =
    when (first()) {
        is TimedWriteResult -> (this as List<TimedWriteResult>).summarize(workloadName)
        is TimedReadResult -> (this as List<TimedReadResult>).summarize(workloadName)
        is TimedMessageResult -> (this as List<TimedMessageResult>).summarize(workloadName)
        is TimedCommandResult -> (this as List<TimedCommandResult>).summarize(workloadName)
    }