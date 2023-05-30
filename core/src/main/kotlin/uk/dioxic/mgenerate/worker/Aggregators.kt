package uk.dioxic.mgenerate.worker

import uk.dioxic.mgenerate.utils.percentile
import uk.dioxic.mgenerate.worker.results.*


fun List<TimedWriteResult>.summarize() =
    SummarizedWriteResult(
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

fun List<TimedMessageResult>.summarize() =
    SummarizedMessageResult(
        msgCount = size,
        latencyPercentiles = listOf(
            "p50" to map { it.duration }.percentile(0.5)
        )
    )

@Suppress("UNCHECKED_CAST")
fun List<TimedWorkloadResult>.summarize(): SummarizedResult =
    when (first()) {
        is TimedWriteResult -> (this as List<TimedWriteResult>).summarize()
        is TimedReadResult -> (this as List<TimedWriteResult>).summarize()
        is TimedMessageResult -> (this as List<TimedMessageResult>).summarize()
    }