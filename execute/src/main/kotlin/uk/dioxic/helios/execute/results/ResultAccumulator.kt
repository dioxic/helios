@file:Suppress("MemberVisibilityCanBePrivate")

package uk.dioxic.helios.execute.results

import com.mongodb.MongoBulkWriteException
import com.mongodb.MongoException
import uk.dioxic.helios.execute.model.ExecutionContext
import kotlin.time.Duration

class ResultAccumulator {
    var insertedCount: Long = 0
        private set
    var matchedCount: Long = 0
        private set
    var modifiedCount: Long = 0
        private set
    var deletedCount: Long = 0
        private set
    var upsertedCount: Long = 0
        private set
    var docsReturned: Int = 0
        private set
    var successCount: Int = 0
        private set
    var failureCount: Int = 0
        private set
    var operationCount: Int = 0
        private set
    var errorCount: Int = 0
        private set
    var elapsedTime: Duration = Duration.ZERO
        private set
    var durations: MutableList<Duration> = mutableListOf()
        private set
    var exceptions: MutableList<MongoException> = mutableListOf()
        private set
    var context: ExecutionContext? = null
        private set

    fun add(timedResult: TimedResult): ResultAccumulator = apply {
        durations.add(timedResult.duration)
        elapsedTime = maxOf(elapsedTime, timedResult.elapsedTime)
        context = timedResult.context
        operationCount++
        when (timedResult) {
            is TimedExecutionResult -> add(timedResult.value)
            is TimedExceptionResult -> add(timedResult.value)
        }
    }

    fun add(exception: MongoException): ResultAccumulator = apply {
        exceptions.add(exception)
        when (exception) {
            is MongoBulkWriteException -> {
                add(exception.writeResult.standardize())
                errorCount += exception.writeErrors.size
            }

            else -> errorCount++
        }
    }

    fun add(executionResult: ExecutionResult): ResultAccumulator = apply {
        when (executionResult) {
            is WriteResult -> {
                insertedCount += executionResult.insertedCount
                matchedCount += executionResult.matchedCount
                modifiedCount += executionResult.modifiedCount
                deletedCount += executionResult.deletedCount
                upsertedCount += executionResult.upsertedCount
            }

            is CommandResult -> {
                successCount += executionResult.success.toInt()
                failureCount += (!executionResult.success).toInt()
            }

            is ReadResult -> {
                docsReturned += executionResult.docsReturned
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