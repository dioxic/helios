package uk.dioxic.mgenerate.execute.results

import uk.dioxic.mgenerate.execute.model.ExecutionContext
import kotlin.time.Duration

data class SummarizedResultsBatch(
    val batchDuration: Duration,
    val results: List<SummarizedResult>
): FrameworkResult

sealed interface SummarizedResult {
    val context: ExecutionContext
    val latencies: SummarizedLatencies
    val operationCount: Int
    val elapsedTime: Duration
}

data class SummarizedLatencies(
    val p50: Duration,
    val p95: Duration,
    val p99: Duration,
    val max: Duration,
    val min: Duration,
)

data class SummarizedWriteResult(
    val insertedCount: Long = 0,
    val matchedCount: Long = 0,
    val modifiedCount: Long = 0,
    val deletedCount: Long = 0,
    val upsertedCount: Long = 0,
    override val context: ExecutionContext,
    override val latencies: SummarizedLatencies,
    override val operationCount: Int = 0,
    override val elapsedTime: Duration,
) : SummarizedResult

data class SummarizedReadResult(
    val docsReturned: Int = 0,
    override val context: ExecutionContext,
    override val latencies: SummarizedLatencies,
    override val operationCount: Int = 0,
    override val elapsedTime: Duration,
) : SummarizedResult

data class SummarizedMessageResult(
    val msgCount: Int = 0,
    override val context: ExecutionContext,
    override val latencies: SummarizedLatencies,
    override val operationCount: Int = 0,
    override val elapsedTime: Duration,
) : SummarizedResult

data class SummarizedCommandResult(
    val successes: Int = 0,
    val failures: Int = 0,
    override val context: ExecutionContext,
    override val latencies: SummarizedLatencies,
    override val operationCount: Int = 0,
    override val elapsedTime: Duration,
) : SummarizedResult