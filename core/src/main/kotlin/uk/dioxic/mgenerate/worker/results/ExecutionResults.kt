package uk.dioxic.mgenerate.worker.results

import org.bson.Document

sealed interface Result

class CommandResult(val document: Document): Result {
    val success: Boolean
        get() = document["ok"]?.let { it == 1 } ?: false
}

data class WriteResult(
    val insertCount: Long = 0,
    val matchedCount: Long = 0,
    val modifiedCount: Long = 0,
    val deletedCount: Long = 0,
    val upsertedCount: Long = 0,
) : Result

data class ReadResult(
    val docReturned: Int = 0,
    val queryCount: Int = 0
) : Result

data class MessageResult(
    val msg: String
) : Result