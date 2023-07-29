package uk.dioxic.helios.execute.format

import com.mongodb.MongoBulkWriteException
import com.mongodb.MongoException
import com.mongodb.MongoServerException
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
    progress = context.executionProgress,
    errorDetail = exceptions.distinctBy { it::class to it.code }.joinToString(", ") { it.toOutputString() },
    elapsed = elapsedTime,
    latencies = latencies
)

private val ExecutionContext.executionProgress
    get() = count.percentOf(workload.count)

private infix fun Int.percentOf(divisor: Int): Percent = (this * 100) / divisor
private infix fun Long.percentOf(divisor: Long): Percent {
    println("percent: $this / $divisor")
    return ((this * 100) / divisor).toInt()
}

fun MongoException.toOutputString() =
    when (this) {
        is MongoBulkWriteException -> this.toOutputString()
        is MongoServerException -> errorCodeName.orEmpty()
        else -> "${this::class.simpleName!!}[${this.message.orEmpty()}]"
    }

fun MongoBulkWriteException.toOutputString(): String {
    this.writeErrors.groupBy { it.code }

    TODO()
}