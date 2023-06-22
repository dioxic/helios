package uk.dioxic.mgenerate.execute.report

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import uk.dioxic.mgenerate.execute.model.ExecutionContext
import uk.dioxic.mgenerate.execute.results.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
data class WorkloadProgressReport(
    @SerialName("workload") val workloadName: String,
    @SerialName("operations") val operationCount: Int,
    @SerialName("progress") val progress: Int,
    @SerialName("elapsed") val elapsed: Duration,
    @SerialName("inserted") val insertedCount: Long = 0L,
    @SerialName("matched") val matchedCount: Long = 0L,
    @SerialName("modified") val modifiedCount: Long = 0L,
    @SerialName("deleted") val deletedCount: Long = 0L,
    @SerialName("upserted") val upsertedCount: Long = 0L,
    @SerialName("docsReturned") val docsReturned: Int = 0,
    @SerialName("successes") val successCount: Int = 0,
    @SerialName("failures") val failureCount: Int = 0,
)

private val json = Json {
    encodeDefaults = false
}

fun WorkloadProgressReport.toMap() =
    json.encodeToJsonElement(this).jsonObject.toMap()

fun SummarizedResultsBatch.toMap() = results.map { sr ->
    with(duration) {
        sr.toReport().toMap().mapValues { (_,v) -> v.jsonPrimitive.content }
    }
}

context(Duration)
private fun SummarizedResult.toReport() = when (this) {
    is SummarizedWriteResult -> toReport()
    is SummarizedCommandResult -> toReport()
    is SummarizedMessageResult -> toReport()
    is SummarizedReadResult -> toReport()
}

context(Duration)
private fun SummarizedWriteResult.toReport() = WorkloadProgressReport(
    workloadName = context.workload.name,
    insertedCount = insertedCount,
    matchedCount = matchedCount,
    modifiedCount = modifiedCount,
    deletedCount = modifiedCount,
    upsertedCount = upsertedCount,
    operationCount = operationCount,
    progress = context.executionProgress,
    elapsed = context.elapsed,
)

context(Duration)
private fun SummarizedReadResult.toReport() = WorkloadProgressReport(
    workloadName = context.workload.name,
    docsReturned = docsReturned,
    operationCount = operationCount,
    progress = context.executionProgress,
    elapsed = context.elapsed,
)

context(Duration)
private fun SummarizedCommandResult.toReport() = WorkloadProgressReport(
    workloadName = context.workload.name,
    successCount = successes,
    failureCount = failures,
    operationCount = operationCount,
    progress = context.executionProgress,
    elapsed = context.elapsed,
)

context(Duration)
private fun SummarizedMessageResult.toReport() = WorkloadProgressReport(
    workloadName = context.workload.name,
    successCount = msgCount,
    operationCount = operationCount,
    progress = context.executionProgress,
    elapsed = context.elapsed,
)

val ExecutionContext.executionProgress
    get() = executionCount.percentOf(workload.count)

val ExecutionContext.elapsed
    get() = (System.currentTimeMillis() - startTimeMillis).toDuration(DurationUnit.MILLISECONDS)

private infix fun Int.percentOf(divisor: Int): Int = (this * 100) / divisor
private infix fun Long.percentOf(divisor: Long): Int = ((this * 100) / divisor).toInt()