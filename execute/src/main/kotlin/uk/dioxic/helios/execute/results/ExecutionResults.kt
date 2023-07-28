package uk.dioxic.helios.execute.results

import org.bson.Document

sealed interface ExecutionResult

data class CommandResult(
    val success: Boolean,
    val document: Document? = null,
) : ExecutionResult {
    companion object {
        operator fun invoke(document: Document): CommandResult =
            CommandResult(
                success = document.isSuccess,
                document = document
            )
    }
}

private val Document.isSuccess: Boolean
    get() = get("ok")?.let {
        when (it) {
            is Number -> (it.toInt() == 1)
            else -> false
        }
    } ?: false

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
    val doc: Map<String, *> = emptyMap<String, Any?>()
) : ExecutionResult

data class TransactionResult(
    val executionResults: List<ExecutionResult>
) : ExecutionResult

data class ErrorResult(
    val error: Throwable
) : ExecutionResult