package uk.dioxic.helios.execute.results

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import uk.dioxic.helios.execute.model.ExecutionContext
import kotlin.time.Duration

data class SummarizedResultsBatch(
    val batchDuration: Duration,
    val results: List<SummarizedResult>
) : FrameworkResult

data class SummarizedResult(
    val insertedCount: Long,
    val matchedCount: Long,
    val modifiedCount: Long,
    val deletedCount: Long,
    val upsertedCount: Long,
    val docsReturned: Int,
    val successCount: Int,
    val failureCount: Int,
    val errorCount: Int,
    val distinctErrors: List<Throwable>,
    val context: ExecutionContext,
    val latencies: SummarizedLatencies,
    val operationCount: Int,
    val elapsedTime: Duration,
)

@Serializable
data class SummarizedLatencies(
    @Contextual val p50: Duration,
    @Contextual val p95: Duration,
    @Contextual val p99: Duration,
    @Contextual val max: Duration,
)