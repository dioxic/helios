package uk.dioxic.mgenerate.execute.results

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
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

@Serializable
data class SummarizedLatencies(
    @Contextual val p50: Duration,
    @Contextual val p95: Duration,
    @Contextual val p99: Duration,
    @Contextual val max: Duration,
)

data class SummarizedWriteResult(
    val insertedCount: Long = 0,
    val matchedCount: Long = 0,
    val modifiedCount: Long = 0,
    val deletedCount: Long = 0,
    val upsertedCount: Long = 0,
    override val context: ExecutionContext,
    override val latencies: SummarizedLatencies,
    override val operationCount: Int,
    override val elapsedTime: Duration,
) : SummarizedResult

data class SummarizedReadResult(
    val docsReturned: Int = 0,
    override val context: ExecutionContext,
    override val latencies: SummarizedLatencies,
    override val operationCount: Int,
    override val elapsedTime: Duration,
) : SummarizedResult

data class SummarizedMessageResult(
    val msgCount: Int = 0,
    override val context: ExecutionContext,
    override val latencies: SummarizedLatencies,
    override val operationCount: Int,
    override val elapsedTime: Duration,
) : SummarizedResult

data class SummarizedCommandResult(
    val successCount: Int = 0,
    val failureCount: Int = 0,
    override val context: ExecutionContext,
    override val latencies: SummarizedLatencies,
    override val operationCount: Int,
    override val elapsedTime: Duration,
) : SummarizedResult

data class SummarizedTransactionResult(
    val insertedCount: Long = 0,
    val matchedCount: Long = 0,
    val modifiedCount: Long = 0,
    val deletedCount: Long = 0,
    val upsertedCount: Long = 0,
    val docsReturned: Int = 0,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    override val context: ExecutionContext,
    override val latencies: SummarizedLatencies,
    override val operationCount: Int,
    override val elapsedTime: Duration,
) : SummarizedResult

