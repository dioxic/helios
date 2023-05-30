package uk.dioxic.mgenerate.worker.results

import kotlin.time.Duration

sealed interface SummarizedResult: OutputResult

data class SummarizedWriteResult(
    override val workloadName: String,
    val insertCount: Long = 0,
    val matchedCount: Long = 0,
    val modifiedCount: Long = 0,
    val deletedCount: Long = 0,
    val upsertedCount: Long = 0,
    val latencyPercentiles: List<Pair<String, Duration>>
) : SummarizedResult

data class SummarizedReadResult(
    override val workloadName: String,
    val docReturned: Int = 0,
    val queryCount: Int = 0,
    val latencyPercentiles: List<Pair<String, Duration>>
) : SummarizedResult

data class SummarizedMessageResult(
    override val workloadName: String,
    val msgCount: Int = 0,
    val latencyPercentiles: List<Pair<String, Duration>>
) : SummarizedResult