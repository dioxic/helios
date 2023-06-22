package uk.dioxic.mgenerate.execute.results

import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.InsertManyResult
import com.mongodb.client.result.InsertOneResult
import com.mongodb.client.result.UpdateResult

fun UpdateResult.standardize() = WriteResult(
    matchedCount = matchedCount,
    modifiedCount = modifiedCount,
    upsertedCount = if (upsertedId == null) 0 else 1
)

fun DeleteResult.standardize() = WriteResult(
    deletedCount = deletedCount
)

fun InsertOneResult.standardize() = WriteResult(
    insertCount = if (insertedId == null) 0 else 1
)

fun InsertManyResult.standardize() = WriteResult(
    insertCount = insertedIds.size.toLong()
)

fun BulkWriteResult.standardize() = WriteResult(
    insertCount = insertedCount.toLong(),
    upsertedCount = upserts.size.toLong(),
    modifiedCount = modifiedCount.toLong(),
    matchedCount = matchedCount.toLong(),
    deletedCount = deletedCount.toLong(),
)