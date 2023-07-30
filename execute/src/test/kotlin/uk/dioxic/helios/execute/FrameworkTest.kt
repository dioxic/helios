package uk.dioxic.helios.execute

import com.mongodb.MongoException
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.withTimeout
import uk.dioxic.helios.execute.model.ExecutionContext
import uk.dioxic.helios.execute.model.InsertOneExecutor
import uk.dioxic.helios.execute.model.MessageExecutor
import uk.dioxic.helios.execute.model.TpsRate
import uk.dioxic.helios.execute.resources.ResourceRegistry
import uk.dioxic.helios.execute.results.MessageResult
import uk.dioxic.helios.execute.results.SummarizedResultsBatch
import uk.dioxic.helios.execute.results.TimedExecutionResult
import uk.dioxic.helios.execute.results.WriteResult
import uk.dioxic.helios.execute.test.IS_NOT_GH_ACTION
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

class FrameworkTest : FunSpec({

    val executor = mockk<MessageExecutor>()

    afterTest {
        clearMocks(executor)
    }

    beforeTest {
        coEvery {
            with(any<ExecutionContext>()) {
                with(any<ResourceRegistry>()) {
                    executor.execute()
                }
            }
        } returns MessageResult()
        every { executor.variablesRequired } returns 1
    }

    context("execution counts") {

        fun verifyExCount(count: Int) = coVerify(exactly = count) {
            with(any<ExecutionContext>()) {
                with(any<ResourceRegistry>()) {
                    executor.execute()
                }
            }
        }

        test("sequential stage has correct execution count") {
            buildBenchmark {
                sequentialStage {
                    addRateWorkload(executor = executor, count = 5)
                    addRateWorkload(executor = executor, count = 5)
                }
            }.execute().count()

            verifyExCount(10)
        }


        test("parallel stage with rate workloads has correct execution count") {

            buildBenchmark {
                parallelStage {
                    addRateWorkload(executor = executor, count = 5)
                    addRateWorkload(executor = executor, count = 5)
                }
            }.execute().count()

            verifyExCount(10)
        }

        test("parallel stage with weighted workloads has correct execution count") {
            buildBenchmark {
                parallelStage {
                    addWeightedWorkload(executor = executor, count = 5)
                    addWeightedWorkload(executor = executor, count = 5)
                }
            }.execute().count()

            verifyExCount(10)
        }

        test("parallel stage with mixed workloads has correct execution count") {
            buildBenchmark {
                parallelStage {
                    addWeightedWorkload(executor = executor, count = 5)
                    addWeightedWorkload(executor = executor, count = 5)
                    addRateWorkload(executor = executor, count = 5)
                }
            }.execute().count()

            verifyExCount(15)
        }
    }

    test("workload tps rate is roughly correct for single execution").config(enabled = IS_NOT_GH_ACTION) {
        val count = 300
        val tps = 100
        val expectedDuration = (count / tps).seconds

        val duration = measureTime {
            buildBenchmark {
                sequentialStage {
                    addRateWorkload(executor = executor, count = count.toLong(), rate = TpsRate(tps))
                }
            }.execute().count()
        }
        println(duration)
        duration shouldBeGreaterThan expectedDuration
        duration shouldBeLessThan expectedDuration * 2
    }

    test("workload tps rate is roughly correct for parallel execution").config(enabled = IS_NOT_GH_ACTION) {
        val count = 300
        val tps = 100
        val expectedDuration = (count / tps).seconds

        val duration = measureTime {
            buildBenchmark {
                parallelStage {
                    addRateWorkload(executor = executor, count = count.toLong(), rate = TpsRate(tps))
                    addRateWorkload(executor = executor, count = count.toLong(), rate = TpsRate(tps))
                    addRateWorkload(executor = executor, count = count.toLong(), rate = TpsRate(tps))
                    addRateWorkload(executor = executor, count = count.toLong(), rate = TpsRate(tps))
                }
            }.execute().count()
        }
        println(duration)
        duration shouldBeGreaterThan expectedDuration
        duration shouldBeLessThan expectedDuration * 2
    }

    test("single executions don't get summarized") {
        buildBenchmark {
            parallelStage {
                addRateWorkload(executor = executor, count = 1)
            }
        }.execute().collect {
            if (it is ProgressMessage) {
                it.result.shouldBeInstanceOf<TimedExecutionResult>()
            }
        }
    }

    test("multiple executions get summarized") {
        buildBenchmark {
            parallelStage {
                addRateWorkload(executor = executor, count = 100)
            }
        }.execute().collect {
            if (it is ProgressMessage) {
                it.result.shouldBeInstanceOf<SummarizedResultsBatch>()
            }
        }
    }

    test("parallel stage time limit is enforced") {
        val timeout = 1.seconds

        withTimeout(timeout * 2) {
            buildBenchmark {
                parallelStage {
                    this.timeout = timeout
                    addRateWorkload(executor = executor, count = 1_000_000, rate = TpsRate(10))
                }
            }.execute().collect {
                if (it is ProgressMessage) {
                    it.result.shouldBeInstanceOf<SummarizedResultsBatch>()
                }
            }
        }
    }

    test("mongo executor is successful") {
        val insertOneExecutor = mockk<InsertOneExecutor>()

        coEvery {
            with(any<ExecutionContext>()) {
                with(any<ResourceRegistry>()) {
                    insertOneExecutor.execute()
                }
            }
        } returns WriteResult(insertedCount = 20)
        every { insertOneExecutor.variablesRequired } returns 1

        buildBenchmark {
            parallelStage {
                addRateWorkload(executor = insertOneExecutor, count = 100, rate = TpsRate(50))
            }
        }.execute().collect {
            if (it is ProgressMessage) {
                it.result.shouldBeInstanceOf<SummarizedResultsBatch>()
            }
        }

    }

    test("errors are handled") {
        coEvery {
            with(any<ExecutionContext>()) {
                with(any<ResourceRegistry>()) {
                    executor.execute()
                }
            }
        } throws MongoException("error")

        buildBenchmark {
            sequentialStage {
                addRateWorkload(executor = executor, count = 100, rate = TpsRate(100))
                addRateWorkload(executor = executor, count = 100, rate = TpsRate(100))
            }
        }.execute().collect {
            println(it)
        }
    }

})