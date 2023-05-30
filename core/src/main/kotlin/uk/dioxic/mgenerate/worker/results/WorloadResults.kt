package uk.dioxic.mgenerate.worker.results

sealed interface WorkloadResult

data class WriteResult(
    val insertCount: Long = 0,
    val matchedCount: Long = 0,
    val modifiedCount: Long = 0,
    val deletedCount: Long = 0,
    val upsertedCount: Long = 0,
) : WorkloadResult

data class ReadResult(
    val docReturned: Int = 0,
    val queryCount: Int = 0
) : WorkloadResult

data class MessageResult(
    val msg: String
): WorkloadResult