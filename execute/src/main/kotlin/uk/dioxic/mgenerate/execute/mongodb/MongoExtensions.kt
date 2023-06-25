package uk.dioxic.mgenerate.execute.mongodb

import arrow.resilience.Schedule
import arrow.resilience.retry
import com.mongodb.MongoException
import com.mongodb.MongoExecutionTimeoutException
import com.mongodb.TransactionOptions
import com.mongodb.client.ClientSession
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
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
        .zipRight(Schedule.doWhile<Throwable> { e, _ ->
            if (hasActiveTransaction()) {
                abortTransaction()
            }
            e is MongoException
                    && e.hasErrorLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL)
                    && startTime.elapsedNow() < maxRetryLimit
        }).retry {
            startTransaction(options)
            val retVal = body.invoke()
            Schedule.doWhile<Throwable> { e, _ ->
                e is MongoException
                        && e !is MongoExecutionTimeoutException
                        && e.hasErrorLabel(MongoException.UNKNOWN_TRANSACTION_COMMIT_RESULT_LABEL)
                        && startTime.elapsedNow() < maxRetryLimit
            }.retry {
                commitTransaction()
            }
            retVal
        }

    return res
}