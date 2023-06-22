package uk.dioxic.mgenerate.execute.results

import uk.dioxic.mgenerate.execute.model.ExecutionContext
import kotlin.time.Duration

data class SummarizedResultsBatch(
    val duration: Duration,
    val results: List<SummarizedResult>
): FrameworkResult

sealed interface SummarizedResult {
    val context: ExecutionContext
    val latencies: SummarizedLatencies
}

data class SummarizedLatencies(
    val p50: Duration,
    val p95: Duration,
    val p99: Duration,
    val max: Duration,
    val min: Duration,
)

data class SummarizedWriteResult(
    override val context: ExecutionContext,
    val insertCount: Long = 0,
    val matchedCount: Long = 0,
    val modifiedCount: Long = 0,
    val deletedCount: Long = 0,
    val upsertedCount: Long = 0,
    override val latencies: SummarizedLatencies
) : SummarizedResult

data class SummarizedReadResult(
    override val context: ExecutionContext,
    val docReturned: Int = 0,
    val queryCount: Int = 0,
    override val latencies: SummarizedLatencies
) : SummarizedResult

data class SummarizedMessageResult(
    override val context: ExecutionContext,
    val msgCount: Int = 0,
    override val latencies: SummarizedLatencies
) : SummarizedResult

data class SummarizedCommandResult(
    override val context: ExecutionContext,
    val successes: Int = 0,
    val failures: Int = 0,
    override val latencies: SummarizedLatencies
) : SummarizedResult