package uk.dioxic.helios.execute.format

import uk.dioxic.helios.execute.model.ExecutionContext
import uk.dioxic.helios.execute.results.*

fun TimedResult.toOutputResult(stageName: String) = when (this) {
    is TimedWriteResult -> this.toOutputResult(stageName)
    is TimedCommandResult -> this.toOutputResult(stageName)
    is TimedMessageResult -> this.toOutputResult(stageName)
    is TimedReadResult -> this.toOutputResult(stageName)
    is TimedTransactionResult -> this.toOutputResult(stageName)
    is TimedErrorResult -> this.toOutputResult(stageName)
}

private fun TimedErrorResult.toOutputResult(stageName: String) = OutputResult(
    stageName = stageName,
    workloadName = context.workload.name,
    operationCount = 1,
    progress = 100,
    elapsed = duration,
    failureCount = 1,
    errorString = value.error.toOutputString()
)

private fun TimedTransactionResult.toOutputResult(stageName: String): OutputResult =
    value.executionResults.fold(ResultAccumulator(), ResultAccumulator::add)
        .toSummarizedResult()
        .toOutputResult(stageName)

private fun TimedWriteResult.toOutputResult(stageName: String) = OutputResult(
    stageName = stageName,
    workloadName = context.workload.name,
    operationCount = 1,
    progress = context.executionProgress,
    elapsed = duration,
    insertedCount = value.insertedCount,
    matchedCount = value.matchedCount,
    modifiedCount = value.modifiedCount,
    deletedCount = value.deletedCount,
    upsertedCount = value.upsertedCount,
)

private fun TimedReadResult.toOutputResult(stageName: String) = OutputResult(
    stageName = stageName,
    workloadName = context.workload.name,
    operationCount = 1,
    progress = context.executionProgress,
    elapsed = duration,
    docsReturned = value.docsReturned,
)

private fun TimedCommandResult.toOutputResult(stageName: String) = OutputResult(
    stageName = stageName,
    workloadName = context.workload.name,
    operationCount = 1,
    progress = context.executionProgress,
    elapsed = duration,
    successCount = value.successCount,
    failureCount = value.failureCount,
)

private fun TimedMessageResult.toOutputResult(stageName: String) = OutputResult(
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
    errorString = distinctErrors.joinToString(", ") { it.toOutputString() },
    elapsed = elapsedTime,
    latencies = latencies
)

private val ExecutionContext.executionProgress
    get() = count.percentOf(workload.count)

private infix fun Int.percentOf(divisor: Int): Percent = (this * 100) / divisor
private infix fun Long.percentOf(divisor: Long): Percent = ((this * 100) / divisor).toInt()

private fun Throwable.toOutputString() =
    "${this::class.simpleName!!}[${this.message.orEmpty()}]"
