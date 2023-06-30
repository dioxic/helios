package uk.dioxic.helios.execute

import arrow.resilience.Schedule
import arrow.resilience.retry
import com.mongodb.MongoException
import com.mongodb.MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import uk.dioxic.helios.execute.model.ExecutionContext
import uk.dioxic.helios.execute.model.MessageExecutor
import uk.dioxic.helios.execute.resources.ResourceRegistry
import uk.dioxic.helios.execute.results.MessageResult

class ArrowRetryTests : FunSpec({

    val mongoTransientTxnEx = MongoException(123, "error")
        .apply {
            addLabel(TRANSIENT_TRANSACTION_ERROR_LABEL)
        }

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
        } throws mongoTransientTxnEx andThen MessageResult()

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
        } throws mongoTransientTxnEx andThen MessageResult()

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

})