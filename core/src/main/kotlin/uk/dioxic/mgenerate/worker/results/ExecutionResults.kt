@file:Suppress("MemberVisibilityCanBePrivate")

package uk.dioxic.mgenerate.worker.results

sealed interface Result

data class CommandResult(val success: Boolean): Result

data class WriteResult(
    val insertCount: Long = 0,
    val matchedCount: Long = 0,
    val modifiedCount: Long = 0,
    val deletedCount: Long = 0,
    val upsertedCount: Long = 0,
) : Result

data class ReadResult(
    val docReturned: Int = 0,
) : Result

data class MessageResult(
    val msg: String
) : Result