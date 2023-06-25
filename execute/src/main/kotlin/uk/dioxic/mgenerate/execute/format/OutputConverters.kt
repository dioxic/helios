package uk.dioxic.mgenerate.execute.format

import uk.dioxic.mgenerate.execute.model.ExecutionContext
import uk.dioxic.mgenerate.execute.results.*
import kotlin.time.Duration

fun TimedResult.toOutputResult(stageName: String) = when (this) {
    is TimedWriteResult -> this.toOutputResult(stageName)
    is TimedCommandResult -> this.toOutputResult(stageName)
    is TimedMessageResult -> this.toOutputResult(stageName)
    is TimedReadResult -> this.toOutputResult(stageName)
    is TimedTransactionResult -> this.toOutputResult(stageName)
}

private fun TimedTransactionResult.toOutputResult(stageName: String): OutputResult {
    val accumulator = value.executionResults.fold(ResultAccumulator()) { acc, res ->
        acc.add(res)
    }

    return OutputResult(
        stageName = stageName,
        workloadName = context.workload.name,
        operationCount = 1,
        progress = 100,
        elapsed = duration,
        insertedCount = accumulator.insertedCount,
        matchedCount = accumulator.matchedCount,
        modifiedCount = accumulator.modifiedCount,
        deletedCount = accumulator.modifiedCount,
        upsertedCount = accumulator.upsertedCount,
        docsReturned = accumulator.docsReturned,
        failureCount = accumulator.failureCount,
        successCount = accumulator.successCount
    )
}

private fun TimedWriteResult.toOutputResult(stageName: String) = OutputResult(
    stageName = stageName,
    workloadName = context.workload.name,
    operationCount = 1,
    progress = 100,
    elapsed = duration,
    insertedCount = value.insertedCount,
    matchedCount = value.matchedCount,
    modifiedCount = value.modifiedCount,
    deletedCount = value.modifiedCount,
    upsertedCount = value.upsertedCount,
)

private fun TimedReadResult.toOutputResult(stageName: String) = OutputResult(
    stageName = stageName,
    workloadName = context.workload.name,
    operationCount = 1,
    progress = 100,
    elapsed = duration,
    docsReturned = value.docsReturned,
)

private fun TimedCommandResult.toOutputResult(stageName: String) = OutputResult(
    stageName = stageName,
    workloadName = context.workload.name,
    operationCount = 1,
    progress = 100,
    elapsed = duration,
    successCount = value.successCount,
    failureCount = value.failureCount,
)

private fun TimedMessageResult.toOutputResult(stageName: String) = OutputResult(
    stageName = stageName,
    workloadName = context.workload.name,
    operationCount = 1,
    progress = 100,
    elapsed = duration,
    successCount = 1
)

context(Duration)
fun SummarizedResult.toOutputResult(stageName: String) = when (this) {
    is SummarizedWriteResult -> toOutputResult(stageName)
    is SummarizedCommandResult -> toOutputResult(stageName)
    is SummarizedMessageResult -> toOutputResult(stageName)
    is SummarizedReadResult -> toOutputResult(stageName)
    is SummarizedTransactionResult -> toOutputResult(stageName)
}

context(Duration)
private fun SummarizedTransactionResult.toOutputResult(stageName: String) = OutputResult(
    stageName = stageName,
    workloadName = context.workload.name,
    insertedCount = insertedCount,
    matchedCount = matchedCount,
    modifiedCount = modifiedCount,
    deletedCount = modifiedCount,
    upsertedCount = upsertedCount,
    docsReturned = docsReturned,
    successCount = successCount,
    failureCount = failureCount,
    operationCount = operationCount,
    progress = context.executionProgress,
    elapsed = elapsedTime,
    latencies = latencies,
)

context(Duration)
private fun SummarizedWriteResult.toOutputResult(stageName: String) = OutputResult(
    stageName = stageName,
    workloadName = context.workload.name,
    insertedCount = insertedCount,
    matchedCount = matchedCount,
    modifiedCount = modifiedCount,
    deletedCount = modifiedCount,
    upsertedCount = upsertedCount,
    operationCount = operationCount,
    progress = context.executionProgress,
    elapsed = elapsedTime,
    latencies = latencies,
)

context(Duration)
private fun SummarizedReadResult.toOutputResult(stageName: String) = OutputResult(
    stageName = stageName,
    workloadName = context.workload.name,
    docsReturned = docsReturned,
    operationCount = operationCount,
    progress = context.executionProgress,
    elapsed = elapsedTime,
    latencies = latencies,
)

context(Duration)
private fun SummarizedCommandResult.toOutputResult(stageName: String) = OutputResult(
    stageName = stageName,
    workloadName = context.workload.name,
    successCount = successCount,
    failureCount = failureCount,
    operationCount = operationCount,
    progress = context.executionProgress,
    elapsed = elapsedTime,
    latencies = latencies,
)

context(Duration)
private fun SummarizedMessageResult.toOutputResult(stageName: String) = OutputResult(
    stageName = stageName,
    workloadName = context.workload.name,
    successCount = msgCount,
    operationCount = operationCount,
    progress = context.executionProgress,
    elapsed = elapsedTime,
    latencies = latencies,
)

private val ExecutionContext.executionProgress
    get() = executionCount.percentOf(workload.count)

private infix fun Int.percentOf(divisor: Int): Percent = (this * 100) / divisor
private infix fun Long.percentOf(divisor: Long): Percent = ((this * 100) / divisor).toInt()