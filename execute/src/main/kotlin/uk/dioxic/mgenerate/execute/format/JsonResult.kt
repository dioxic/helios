package uk.dioxic.mgenerate.execute.format

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import uk.dioxic.mgenerate.execute.model.ExecutionContext
import uk.dioxic.mgenerate.execute.results.*
import uk.dioxic.mgenerate.execute.serialization.DurationConsoleSerializer
import uk.dioxic.mgenerate.execute.serialization.IntPercentSerializer
import kotlin.time.Duration

typealias ResultsMap =  List<Map<String, String>>
typealias Percent = Int

@Serializable
private data class JsonResult(
    @SerialName("workload") val workloadName: String,
    @SerialName("operations") val operationCount: Int,
    @SerialName("inserted") val insertedCount: Long = 0L,
    @SerialName("matched") val matchedCount: Long = 0L,
    @SerialName("modified") val modifiedCount: Long = 0L,
    @SerialName("deleted") val deletedCount: Long = 0L,
    @SerialName("upserted") val upsertedCount: Long = 0L,
    @SerialName("docsReturned") val docsReturned: Int = 0,
    @SerialName("successes") val successCount: Int = 0,
    @SerialName("failures") val failureCount: Int = 0,
    @SerialName("elapsed") @Contextual val elapsed: Duration,
    @SerialName("progress") @Contextual val progress: Percent,
)

val resultFieldOrder = JsonResult(
    workloadName = "",
    operationCount = 0,
    elapsed = Duration.ZERO,
    progress = 100
).toMap(Json { encodeDefaults = true })
    .map { (k, _) -> k }
    .mapIndexed { i, s -> s to i }
    .toMap()

private val json = Json {
    encodeDefaults = false
    serializersModule = SerializersModule {
        contextual(DurationConsoleSerializer)
        contextual(IntPercentSerializer)
    }
}

private fun JsonResult.toMap(json: Json): Map<String, JsonElement> =
    json.encodeToJsonElement(this).jsonObject.toMap()

fun SummarizedResultsBatch.toMap(): ResultsMap = results.map { sr ->
    with(batchDuration) {
        sr.toJsonResult().toMap(json).mapValues { (_,v) -> v.jsonPrimitive.content }
    }
}

context(Duration)
private fun SummarizedResult.toJsonResult() = when (this) {
    is SummarizedWriteResult -> toJsonResult()
    is SummarizedCommandResult -> toJsonResult()
    is SummarizedMessageResult -> toJsonResult()
    is SummarizedReadResult -> toJsonResult()
}

context(Duration)
private fun SummarizedWriteResult.toJsonResult() = JsonResult(
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
private fun SummarizedReadResult.toJsonResult() = JsonResult(
    workloadName = context.workload.name,
    docsReturned = docsReturned,
    operationCount = operationCount,
    progress = context.executionProgress,
    elapsed = elapsedTime,
)

context(Duration)
private fun SummarizedCommandResult.toJsonResult() = JsonResult(
    workloadName = context.workload.name,
    successCount = successes,
    failureCount = failures,
    operationCount = operationCount,
    progress = context.executionProgress,
    elapsed = elapsedTime,
)

context(Duration)
private fun SummarizedMessageResult.toJsonResult() = JsonResult(
    workloadName = context.workload.name,
    successCount = msgCount,
    operationCount = operationCount,
    progress = context.executionProgress,
    elapsed = elapsedTime,
)

val ExecutionContext.executionProgress
    get() = executionCount.percentOf(workload.count)

private infix fun Int.percentOf(divisor: Int): Percent = (this * 100) / divisor
private infix fun Long.percentOf(divisor: Long): Percent = ((this * 100) / divisor).toInt()