package uk.dioxic.mgenerate.execute.results

import org.bson.Document

sealed interface ExecutionResult

data class CommandResult(
    val successCount: Int,
    val failureCount: Int,
    val document: Document? = null,
) : ExecutionResult {
    companion object {
        operator fun invoke(document: Document): CommandResult =
            CommandResult(
                successCount = document.isSuccess.toInt(),
                failureCount = (!document.isSuccess).toInt(),
                document = document
            )
    }
}

val Document.isSuccess: Boolean
    get() = getBoolean("ok", false)

fun Boolean.toInt() =
    when (this) {
        true -> 1
        false -> 0
    }

data class WriteResult(
    val insertedCount: Long = 0,
    val matchedCount: Long = 0,
    val modifiedCount: Long = 0,
    val deletedCount: Long = 0,
    val upsertedCount: Long = 0,
) : ExecutionResult

data class ReadResult(
    val docsReturned: Int = 0,
) : ExecutionResult

data class MessageResult(
    val msg: String
) : ExecutionResult

data class TransactionResult(
    val executionResults: List<ExecutionResult>
) : ExecutionResult
