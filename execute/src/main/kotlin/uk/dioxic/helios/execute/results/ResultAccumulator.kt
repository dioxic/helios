package uk.dioxic.helios.execute.results

import com.mongodb.MongoException
import uk.dioxic.helios.execute.model.ExecutionContext
import kotlin.time.Duration

class ResultAccumulator {
    private var insertedCount: Long = 0
    private var matchedCount: Long = 0
    private var modifiedCount: Long = 0
    private var deletedCount: Long = 0
    private var upsertedCount: Long = 0
    private var docsReturned: Int = 0
    private var successCount: Int = 0
    private var failureCount: Int = 0
    private var operationCount: Int = 0
    private var elapsedTime: Duration = Duration.ZERO
    private var durations: MutableList<Duration> = mutableListOf()
    private var errors: MutableList<MongoException> = mutableListOf()
    private var context: ExecutionContext? = null

    fun add(timedResult: TimedExecutionResult): ResultAccumulator = apply {
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
                successCount += executionResult.success.toInt()
                failureCount += (!executionResult.success).toInt()
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

    fun toSummarizedResult(): SummarizedResult {
        requireNotNull(context) {
            "execution context cannot be null"
        }
        return SummarizedResult(
            insertedCount = insertedCount,
            matchedCount = matchedCount,
            modifiedCount = modifiedCount,
            deletedCount = deletedCount,
            upsertedCount = upsertedCount,
            docsReturned = docsReturned,
            successCount = successCount,
            failureCount= failureCount,
            operationCount = operationCount,
            elapsedTime = elapsedTime,
            latencies = durations.summarize(),
            errors = errors,
            context = context!!,
        )
    }

}