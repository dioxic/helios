package uk.dioxic.helios.execute.mongodb

import arrow.resilience.Schedule
import arrow.resilience.retry
import com.mongodb.MongoException
import com.mongodb.MongoExecutionTimeoutException
import com.mongodb.TransactionOptions
import com.mongodb.client.ClientSession
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import org.bson.Document
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

fun MongoClient.cached() =
    CachedMongoClient(this)

fun MongoDatabase.cached() =
    CachedMongoDatabase(this)

suspend fun <T> ClientSession.withTransaction(
    options: TransactionOptions = TransactionOptions.builder().build(),
    maxRetryLimit: Duration = 120.seconds,
    maxRetryAttempts: Int = Int.MAX_VALUE,
    body: suspend () -> T
): T {
    val startTime = TimeSource.Monotonic.markNow()

    val res = Schedule.recurs<Throwable>(maxRetryAttempts.toLong())
        .zipRight(Schedule.doWhile { e, _ ->
            if (hasActiveTransaction()) {
                abortTransaction()
            }
            e is MongoException
                    && e.hasErrorLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL)
                    && startTime.elapsedNow() < maxRetryLimit
        }).retry {
            startTransaction(options)
            val retVal = body.invoke()
            Schedule.recurs<Throwable>(maxRetryAttempts.toLong())
                .zipRight(Schedule.doWhile { e, _ ->
                    e is MongoException
                            && e !is MongoExecutionTimeoutException
                            && e.hasErrorLabel(MongoException.UNKNOWN_TRANSACTION_COMMIT_RESULT_LABEL)
                            && startTime.elapsedNow() < maxRetryLimit
                }).retry {
                    commitTransaction()
                }
            retVal
        }

    return res
}

/**
 * Convert a list of fields to a projection (fields are included).
 *
 * The _id is excluded by default.
 *
 * Example:
 * ```
 * ["a", "b", "c"] -> { "_id": -1, "a": 1, "b": 1, "c": 1 }
 * ```
 */
fun List<String>?.toProjection(): Document? =
    this?.fold(Document("_id", -1)) { acc, s ->
        acc[s] = 1
        acc
    }