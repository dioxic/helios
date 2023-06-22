package uk.dioxic.mgenerate.execute.results

import org.bson.Document

sealed interface ExecutionResult

data class CommandResult(val document: Document): ExecutionResult {
    val success: Boolean
        get() = document["ok"] == 1
}

data class WriteResult(
    val insertedCount: Long = 0,
    val matchedCount: Long = 0,
    val modifiedCount: Long = 0,
    val deletedCount: Long = 0,
    val upsertedCount: Long = 0,
) : ExecutionResult

data class ReadResult(
    val docReturned: Int = 0,
) : ExecutionResult

data class MessageResult(
    val msg: String
) : ExecutionResult