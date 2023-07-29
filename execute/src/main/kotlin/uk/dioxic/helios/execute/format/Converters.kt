package uk.dioxic.helios.execute.format

import uk.dioxic.helios.execute.model.ExecutionContext
import uk.dioxic.helios.execute.results.*

fun TimedExecutionResult.toOutputResult(stageName: String): OutputResult =
    when (value) {
        is WriteResult -> value.toOutputResult(stageName)
        is CommandResult -> value.toOutputResult(stageName)
        is MessageResult -> value.toOutputResult(stageName)
        is ReadResult -> value.toOutputResult(stageName)
        is TransactionResult -> value.toOutputResult(stageName)
        is ErrorResult -> value.toOutputResult(stageName)
    }

context(TimedExecutionResult)
private fun ErrorResult.toOutputResult(stageName: String) = OutputResult(
    stageName = stageName,
    workloadName = context.workload.name,
    operationCount = 1,
    progress = 100,
    elapsed = duration,
    failureCount = 1,
    errorString = error.toOutputString()
)

context(TimedExecutionResult)
private fun TransactionResult.toOutputResult(stageName: String): OutputResult =
    executionResults.fold(ResultAccumulator(), ResultAccumulator::add)
        .toSummarizedResult()
        .toOutputResult(stageName)

context(TimedExecutionResult)
private fun WriteResult.toOutputResult(stageName: String) = OutputResult(
    stageName = stageName,
    workloadName = context.workload.name,
    operationCount = 1,
    progress = context.executionProgress,
    elapsed = duration,
    insertedCount = insertedCount,
    matchedCount = matchedCount,
    modifiedCount = modifiedCount,
    deletedCount = deletedCount,
    upsertedCount = upsertedCount,
)

context(TimedExecutionResult)
private fun ReadResult.toOutputResult(stageName: String) = OutputResult(
    stageName = stageName,
    workloadName = context.workload.name,
    operationCount = 1,
    progress = context.executionProgress,
    elapsed = duration,
    docsReturned = docsReturned,
)

context(TimedExecutionResult)
private fun CommandResult.toOutputResult(stageName: String) = OutputResult(
    stageName = stageName,
    workloadName = context.workload.name,
    operationCount = 1,
    progress = context.executionProgress,
    elapsed = duration,
    successCount = success.toInt(),
    failureCount = (!success).toInt(),
    errorString = if (!success && document != null) {
        document.toString()
    } else {
        ""
    }
)

context(TimedExecutionResult)
private fun MessageResult.toOutputResult(stageName: String) = OutputResult(
    stageName = stageName,
    workloadName = context.workload.name,
    operationCount = 1,
    progress = context.executionProgress,
    elapsed = duration,
    successCount = 1
)

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
    errorString = errors.distinctBy { it::class to it.message }.joinToString(", ") { it.toOutputString() },
    elapsed = elapsedTime,
    latencies = latencies
)

private val ExecutionContext.executionProgress
    get() = count.percentOf(workload.count)

private infix fun Int.percentOf(divisor: Int): Percent = (this * 100) / divisor
private infix fun Long.percentOf(divisor: Long): Percent = ((this * 100) / divisor).toInt()

private fun Throwable.toOutputString() =
    "${this::class.simpleName!!}[${this.message.orEmpty()}]"
