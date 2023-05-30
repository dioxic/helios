package uk.dioxic.mgenerate.worker.results

import kotlin.time.Duration

sealed interface SummarizedResult

data class SummarizedWriteResult(
    val insertCount: Long = 0,
    val matchedCount: Long = 0,
    val modifiedCount: Long = 0,
    val deletedCount: Long = 0,
    val upsertedCount: Long = 0,
    val latencyPercentiles: List<Pair<String, Duration>>
) : SummarizedResult

data class SummarizedReadResult(
    val docReturned: Int = 0,
    val queryCount: Int = 0,
    val latencyPercentiles: List<Pair<String, Duration>>
) : SummarizedResult

data class SummarizedMessageResult(
    val msgCount: Int = 0,
    val latencyPercentiles: List<Pair<String, Duration>>
) : SummarizedResult