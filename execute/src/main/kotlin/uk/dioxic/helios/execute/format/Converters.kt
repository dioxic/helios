package uk.dioxic.helios.execute.format

import com.mongodb.MongoBulkWriteException
import com.mongodb.MongoException
import com.mongodb.MongoServerException
import com.mongodb.WriteError
import kotlinx.serialization.bson.Bson
import kotlinx.serialization.bson.internal.decodeFromStream
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import uk.dioxic.helios.execute.model.ExecutionContext
import uk.dioxic.helios.execute.results.ResultAccumulator
import uk.dioxic.helios.execute.results.SummarizedResult
import uk.dioxic.helios.execute.results.TimedResult

fun TimedResult.toOutputResult(stageName: String): OutputResult =
    ResultAccumulator().add(this).toOutputResult(stageName)

fun ResultAccumulator.toOutputResult(stageName: String): OutputResult {
    requireNotNull(context) {
        "execution context cannot be null"
    }
    return OutputResult(
        stageName = stageName,
        workloadName = context!!.workload.name,
        insertedCount = insertedCount,
        matchedCount = matchedCount,
        modifiedCount = modifiedCount,
        deletedCount = deletedCount,
        upsertedCount = upsertedCount,
        docsReturned = docsReturned,
        successCount = successCount,
        failureCount = failureCount,
        operationCount = operationCount,
        errorCount = errorCount,
        progress = context!!.executionProgress,
        errorDetail = exceptions.distinctBy { it::class to it.code }.joinToString(", ") { it.toOutputString() },
        elapsed = elapsedTime,
    )
}

fun SummarizedResult.toOutputResult(stageName: String) = OutputResult(
    stageName = stageName,
    workloadName = context.workload.name,
    insertedCount = insertedCount,
    matchedCount = matchedCount,
    modifiedCount = modifiedCount,
    deletedCount = deletedCount,
    upsertedCount = upsertedCount,
    docsReturned = docsReturned,
    successCount = successCount,
    failureCount = failureCount,
    operationCount = operationCount,
    errorCount = errorCount,
    progress = context.executionProgress,
    errorDetail = exceptions.distinctBy { it::class to it.code }.joinToString(", ") { it.toOutputString() },
    elapsed = elapsedTime,
    latencies = latencies
)

private val ExecutionContext.executionProgress
    get() = count.percentOf(workload.count)

private infix fun Int.percentOf(divisor: Int): Percent = (this * 100) / divisor
private infix fun Long.percentOf(divisor: Long): Percent = ((this * 100) / divisor).toInt()

fun MongoException.toOutputString() =
    when (this) {
        is MongoBulkWriteException -> this.toOutputString()
        is MongoServerException -> errorCodeName ?: getErrorName()
        else -> "${this::class.simpleName!!}[${this.message.orEmpty()}]"
    }

fun MongoBulkWriteException.toOutputString(): String = buildString {
    append(writeErrors.distinctBy { it.code }.joinToString(", ") { it.getErrorName() })

    writeConcernError?.also {
        append(getCause(code))
    }
}

fun MongoException.getErrorName() = getCause(code)
fun WriteError.getErrorName() = getCause(code)

private fun getCause(code: Int) =
    mongoErrorCodes[code] ?: code.toString()

val mongoErrorCodes =
    FileSystem.RESOURCES.source("mongoErrorCodes.json".toPath()).buffer().use {
        Bson.decodeFromStream(MapSerializer(String.serializer(), String.serializer()), it.inputStream())
    }.mapKeys { (k,_) -> k.toInt() }
