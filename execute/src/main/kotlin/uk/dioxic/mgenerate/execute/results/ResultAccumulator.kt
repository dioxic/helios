package uk.dioxic.mgenerate.execute.results

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

    fun add(executionResult: ExecutionResult) = apply {
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
            else -> {}
        }
    }

}