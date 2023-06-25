package uk.dioxic.mgenerate.execute

import arrow.resilience.Schedule
import arrow.resilience.retry
import com.mongodb.MongoException
import com.mongodb.MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL
import com.mongodb.MongoException.UNKNOWN_TRANSACTION_COMMIT_RESULT_LABEL
import com.mongodb.MongoExecutionTimeoutException
import com.mongodb.TransactionOptions
import com.mongodb.client.ClientSession
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.result.InsertOneResult
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.bson.BsonObjectId
import uk.dioxic.mgenerate.execute.model.ExecutionContext
import uk.dioxic.mgenerate.execute.model.MessageExecutor
import uk.dioxic.mgenerate.execute.model.TransactionExecutor
import uk.dioxic.mgenerate.execute.resources.ResourceRegistry
import uk.dioxic.mgenerate.execute.results.MessageResult
import uk.dioxic.mgenerate.execute.results.TransactionResult
import uk.dioxic.mgenerate.execute.results.WriteResult
import uk.dioxic.mgenerate.template.Template
import kotlin.time.Duration.Companion.seconds

class RetryTests : FunSpec({

    val mongoTransientTxnEx = MongoException(123, "error")
        .apply {
            addLabel(TRANSIENT_TRANSACTION_ERROR_LABEL)
        }

    val mongoUnknownTxnCommitResultEx = MongoException(456, "error").apply {
        addLabel(UNKNOWN_TRANSACTION_COMMIT_RESULT_LABEL)
    }

    val mongoExecutionTimeoutEx = MongoExecutionTimeoutException(456, "error").apply {
        addLabel(UNKNOWN_TRANSACTION_COMMIT_RESULT_LABEL)
    }

    context("arrow schedule retries") {
        val schedule = Schedule.recurs<Throwable>(3)
            .zipRight(Schedule.doWhile { e, _ ->
                e is MongoException && e.hasErrorLabel(TRANSIENT_TRANSACTION_ERROR_LABEL)
            })

        test("recurs success") {
            val executor = mockk<MessageExecutor>()

            coEvery {
                with(any<ExecutionContext>()) {
                    with(any<ResourceRegistry>()) {
                        executor.execute()
                    }
                }
            } throws mongoTransientTxnEx andThen MessageResult("hello")

            val res = schedule.retry {
                with(defaultExecutionContext) {
                    with(ResourceRegistry()) {
                        executor.execute()
                    }
                }
            }

            coVerify(exactly = 2) {
                with(any<ExecutionContext>()) {
                    with(any<ResourceRegistry>()) {
                        executor.execute()
                    }
                }
            }

            println(res)
        }

        test("retries 3 times then throws for MongoExceptions") {
            val executor = mockk<MessageExecutor>()

            coEvery {
                with(any<ExecutionContext>()) {
                    with(any<ResourceRegistry>()) {
                        executor.execute()
                    }
                }
            } throws mongoTransientTxnEx

            shouldThrowExactly<MongoException> {
                schedule.retry {
                    with(defaultExecutionContext) {
                        with(ResourceRegistry()) {
                            executor.execute()
                        }
                    }
                }
            }

            coVerify(exactly = 4) {
                with(any<ExecutionContext>()) {
                    with(any<ResourceRegistry>()) {
                        executor.execute()
                    }
                }
            }
        }

        test("fails for a non-mongo exception") {
            val executor = mockk<MessageExecutor>()

            coEvery {
                with(any<ExecutionContext>()) {
                    with(any<ResourceRegistry>()) {
                        executor.execute()
                    }
                }
            } throws mongoTransientTxnEx andThenThrows RuntimeException("rte")

            shouldThrowExactly<RuntimeException> {
                schedule.retry {
                    with(defaultExecutionContext) {
                        with(ResourceRegistry()) {
                            executor.execute()
                        }
                    }
                }
            }

            coVerify(exactly = 2) {
                with(any<ExecutionContext>()) {
                    with(any<ResourceRegistry>()) {
                        executor.execute()
                    }
                }
            }
        }

        test("recurs mongo tx") {
            val executor = mockk<MessageExecutor>()

            coEvery {
                with(any<ExecutionContext>()) {
                    with(any<ResourceRegistry>()) {
                        executor.execute()
                    }
                }
            } throws mongoTransientTxnEx andThen MessageResult("hello")

            schedule.retry {
                with(defaultExecutionContext) {
                    with(ResourceRegistry()) {
                        executor.execute()
                    }
                }
            }.shouldBeInstanceOf<MessageResult>()

            coVerify(exactly = 2) {
                with(any<ExecutionContext>()) {
                    with(any<ResourceRegistry>()) {
                        executor.execute()
                    }
                }
            }
        }
    }


    context("mongo transaction") {
        val collection = mockk<MongoCollection<Template>>()
        val session = mockk<ClientSession>(relaxed = true)
        val client = mockk<MongoClient> {
            every { getDatabase(any()).getCollection(any(), any<Class<Template>>()) } returns collection
            every { startSession() } returns session
        }

        afterTest {
            clearMocks(session, collection)
        }

        test("successful execution") {
            every {
                collection.insertOne(any<ClientSession>(), any())
            } returns InsertOneResult.acknowledged(BsonObjectId())

            every { session.hasActiveTransaction() } returns true
            val registry = ResourceRegistry(client)

            val txExecutor = TransactionExecutor(
                executors = listOf(defaultMongoExecutor),
                options = TransactionOptions.builder().build()
            )

            with(defaultExecutionContext) {
                with(registry) {
                    txExecutor.execute()
                }
            }.should {
                it.shouldBeInstanceOf<TransactionResult>()
                it.executionResults.should { exRes ->
                    exRes shouldHaveSize 1
                    exRes.first().shouldBeInstanceOf<WriteResult>()
                }
            }

            verify(inverse = true) { session.abortTransaction() }
            verify(exactly = 1) { session.commitTransaction() }
            verify(exactly = 1) { session.startTransaction(any()) }
            verify(exactly = 1) { session.close() }
        }

        test("fails when executor always throws transient ex") {
            every {
                collection.insertOne(any<ClientSession>(), any())
            } throws mongoTransientTxnEx

            every { session.hasActiveTransaction() } returns true
            val registry = ResourceRegistry(client)

            val txExecutor = TransactionExecutor(
                executors = listOf(defaultMongoExecutor),
                options = TransactionOptions.builder().build(),
                maxRetryAttempts = 10
            )

            runTest {
                withTimeout(3.seconds) {
                    shouldThrowExactly<MongoException> {
                        with(defaultExecutionContext) {
                            with(registry) {
                                txExecutor.execute()
                            }
                        }
                    }
                }
            }

            verify(atLeast = 1) { session.abortTransaction() }
            verify(inverse = true) { session.commitTransaction() }
            verify(atLeast = 1) { session.startTransaction(any()) }
            verify(exactly = 1) { session.close() }
        }

        test("fails immediately on non-mongo exception") {
            every {
                collection.insertOne(any<ClientSession>(), any())
            } throws RuntimeException()

            every { session.hasActiveTransaction() } returns true

            val registry = ResourceRegistry(client)

            val txExecutor = TransactionExecutor(
                executors = listOf(defaultMongoExecutor),
                options = TransactionOptions.builder().build(),
                maxRetryTimeout = 1.seconds
            )

            shouldThrowExactly<RuntimeException> {
                with(defaultExecutionContext) {
                    with(registry) {
                        txExecutor.execute()
                    }
                }
            }

            verify(exactly = 1) { session.abortTransaction() }
            verify(inverse = true) { session.commitTransaction() }
            verify(exactly = 1) { session.startTransaction(any()) }
            verify(exactly = 1) { session.close() }
        }

        test("succeeds after 1 transient ex on execution") {
            every {
                collection.insertOne(any<ClientSession>(), any())
            } throws mongoTransientTxnEx andThen InsertOneResult.acknowledged(BsonObjectId())

            every { session.hasActiveTransaction() } returns true

            val registry = ResourceRegistry(client)

            val txExecutor = TransactionExecutor(
                executors = listOf(defaultMongoExecutor),
                options = TransactionOptions.builder().build(),
                maxRetryTimeout = 1.seconds
            )

            with(defaultExecutionContext) {
                with(registry) {
                    txExecutor.execute()
                }
            }.shouldBeInstanceOf<TransactionResult>()

            verify(exactly = 1) { session.abortTransaction() }
            verify(exactly = 1) { session.commitTransaction() }
            verify(exactly = 2) { session.startTransaction(any()) }
            verify(exactly = 1) { session.close() }
        }

        test("succeeds after 1 unknown commit result exception") {
            every {
                collection.insertOne(any<ClientSession>(), any())
            } returns InsertOneResult.acknowledged(BsonObjectId())

            every { session.hasActiveTransaction() } returns true
            every { session.commitTransaction() } throws mongoUnknownTxnCommitResultEx andThen Unit

            val registry = ResourceRegistry(client)

            val txExecutor = TransactionExecutor(
                executors = listOf(defaultMongoExecutor),
                options = TransactionOptions.builder().build(),
                maxRetryTimeout = 1.seconds
            )

            with(defaultExecutionContext) {
                with(registry) {
                    txExecutor.execute()
                }
            }.shouldBeInstanceOf<TransactionResult>()

            verify(inverse = true) { session.abortTransaction() }
            verify(exactly = 2) { session.commitTransaction() }
            verify(exactly = 1) { session.startTransaction(any()) }
            verify(exactly = 1) { session.close() }
        }

        test("succeeds after 1 transient ex on commit") {
            every {
                collection.insertOne(any<ClientSession>(), any())
            } returns InsertOneResult.acknowledged(BsonObjectId())

            every { session.hasActiveTransaction() } returns true
            every { session.commitTransaction() } throws mongoTransientTxnEx andThen Unit

            val registry = ResourceRegistry(client)

            val txExecutor = TransactionExecutor(
                executors = listOf(defaultMongoExecutor),
                options = TransactionOptions.builder().build(),
                maxRetryTimeout = 1.seconds
            )

            with(defaultExecutionContext) {
                with(registry) {
                    txExecutor.execute()
                }
            }.shouldBeInstanceOf<TransactionResult>()

            verify(exactly = 1) { session.abortTransaction() }
            verify(exactly = 2) { session.commitTransaction() }
            verify(exactly = 2) { session.startTransaction(any()) }
            verify(exactly = 1) { session.close() }
        }

        test("fails on execution timeout ex on commit") {
            every {
                collection.insertOne(any<ClientSession>(), any())
            } returns InsertOneResult.acknowledged(BsonObjectId())

            every { session.hasActiveTransaction() } returns true
            every { session.commitTransaction() } throws mongoExecutionTimeoutEx

            val registry = ResourceRegistry(client)

            val txExecutor = TransactionExecutor(
                executors = listOf(defaultMongoExecutor),
                options = TransactionOptions.builder().build(),
                maxRetryTimeout = 1.seconds
            )

            shouldThrowExactly<MongoExecutionTimeoutException> {
                with(defaultExecutionContext) {
                    with(registry) {
                        txExecutor.execute()
                    }
                }
            }

            verify(exactly = 1) { session.abortTransaction() }
            verify(exactly = 1) { session.commitTransaction() }
            verify(exactly = 1) { session.startTransaction(any()) }
            verify(exactly = 1) { session.close() }
        }
    }

})