package uk.dioxic.mgenerate.execute.format

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import uk.dioxic.mgenerate.execute.model.ExecutionContext
import uk.dioxic.mgenerate.execute.results.*
import kotlin.time.Duration

typealias ResultsMap =  List<Map<String, String>>

@Serializable
data class JsonResult(
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

fun JsonResult.toMap(): Map<String, JsonElement> =
    json.encodeToJsonElement(this).jsonObject.toMap()

fun SummarizedResultsBatch.toMap(): ResultsMap = results.map { sr ->
    with(batchDuration) {
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
private fun SummarizedWriteResult.toReport() = JsonResult(
    workloadName = context.workload.name,
    insertedCount = insertedCount,
    matchedCount = matchedCount,
    modifiedCount = modifiedCount,
    deletedCount = modifiedCount,
    upsertedCount = upsertedCount,
    operationCount = operationCount,
    progress = context.executionProgress,
    elapsed = elapsedTime,
)

context(Duration)
private fun SummarizedReadResult.toReport() = JsonResult(
    workloadName = context.workload.name,
    docsReturned = docsReturned,
    operationCount = operationCount,
    progress = context.executionProgress,
    elapsed = elapsedTime,
)

context(Duration)
private fun SummarizedCommandResult.toReport() = JsonResult(
    workloadName = context.workload.name,
    successCount = successes,
    failureCount = failures,
    operationCount = operationCount,
    progress = context.executionProgress,
    elapsed = elapsedTime,
)

context(Duration)
private fun SummarizedMessageResult.toReport() = JsonResult(
    workloadName = context.workload.name,
    successCount = msgCount,
    operationCount = operationCount,
    progress = context.executionProgress,
    elapsed = elapsedTime,
)

val ExecutionContext.executionProgress
    get() = executionCount.percentOf(workload.count)

private infix fun Int.percentOf(divisor: Int): Int = (this * 100) / divisor
private infix fun Long.percentOf(divisor: Long): Int = ((this * 100) / divisor).toInt()