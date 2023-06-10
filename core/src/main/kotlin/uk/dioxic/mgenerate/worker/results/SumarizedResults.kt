package uk.dioxic.mgenerate.worker.results

import kotlin.time.Duration

sealed interface SummarizedResult {
    val workloadName: String
    val latencyPercentiles: List<Pair<String, Duration>>
}

data class SummarizedWriteResult(
    override val workloadName: String,
    val insertCount: Long = 0,
    val matchedCount: Long = 0,
    val modifiedCount: Long = 0,
    val deletedCount: Long = 0,
    val upsertedCount: Long = 0,
    override val latencyPercentiles: List<Pair<String, Duration>>
) : SummarizedResult

data class SummarizedReadResult(
    override val workloadName: String,
    val docReturned: Int = 0,
    val queryCount: Int = 0,
    override val latencyPercentiles: List<Pair<String, Duration>>
) : SummarizedResult

data class SummarizedMessageResult(
    override val workloadName: String,
    val msgCount: Int = 0,
    override val latencyPercentiles: List<Pair<String, Duration>>
) : SummarizedResult

data class SummarizedCommandResult(
    override val workloadName: String,
    val successes: Int = 0,
    val failures: Int = 0,
    override val latencyPercentiles: List<Pair<String, Duration>>
) : SummarizedResult