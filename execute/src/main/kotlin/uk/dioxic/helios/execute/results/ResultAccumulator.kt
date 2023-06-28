package uk.dioxic.helios.execute.results

import uk.dioxic.helios.execute.model.ExecutionContext
import kotlin.time.Duration

class ResultAccumulator {
    var insertedCount: Long = 0
    var matchedCount: Long = 0
    var modifiedCount: Long = 0
    var deletedCount: Long = 0
    var upsertedCount: Long = 0
    var docsReturned: Int = 0
    var successCount: Int = 0
    var failureCount: Int = 0
    var operationCount: Int = 0
    var elapsedTime: Duration = Duration.ZERO
    var durations: MutableList<Duration> = mutableListOf()
    var errors: MutableList<Throwable> = mutableListOf()
    var context: ExecutionContext? = null

    fun add(timedResult: TimedResult): ResultAccumulator = apply {
        durations.add(timedResult.duration)
        elapsedTime = maxOf(elapsedTime, timedResult.elapsedTime)
        context = timedResult.context
        add(timedResult.value)
    }

    fun add(executionResult: ExecutionResult): ResultAccumulator = apply {
        operationCount++

        when (executionResult) {
            is WriteResult -> {
                insertedCount += executionResult.insertedCount
                matchedCount += executionResult.matchedCount
                modifiedCount += executionResult.modifiedCount
                deletedCount += executionResult.deletedCount
                upsertedCount += executionResult.upsertedCount
            }

            is CommandResult -> {
                successCount += executionResult.successCount
                failureCount += executionResult.failureCount
            }

            is ReadResult -> {
                docsReturned += executionResult.docsReturned
            }

            is ErrorResult -> {
                errors.add(executionResult.error)
            }

            is TransactionResult -> {
                executionResult.executionResults.forEach(::add)
            }

            is MessageResult -> {
                successCount++
            }
        }
    }

}