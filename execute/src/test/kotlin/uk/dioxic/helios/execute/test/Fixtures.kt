package uk.dioxic.helios.execute.test

import com.mongodb.MongoBulkWriteException
import com.mongodb.ServerAddress
import com.mongodb.bulk.*
import org.bson.BsonObjectId

fun bulkWriteResult(
    insertCount: Int = 0,
    matchedCount: Int = 0,
    modifiedCount: Int = 0,
    deletedCount: Int = 0,
    upsertedCount: Int = 0
) = BulkWriteResult.acknowledged(
    insertCount,
    matchedCount,
    deletedCount,
    modifiedCount,
    List(upsertedCount) {
        BulkWriteUpsert(it, BsonObjectId())
    },
    List(insertCount) {
        BulkWriteInsert(it, BsonObjectId())
    }
)

fun mongoBulkWriteException(
    writeResult: BulkWriteResult = bulkWriteResult(),
    writeErrors: List<BulkWriteError> = emptyList(),
    writeConcernError: WriteConcernError? = null,
    serverAddress: ServerAddress = ServerAddress(),
    errorLabels: Set<String> = emptySet()
) = MongoBulkWriteException(
    writeResult,
    writeErrors,
    writeConcernError,
    serverAddress,
    errorLabels
)