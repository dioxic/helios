package uk.dioxic.helios.execute

import com.mongodb.MongoException
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.maps.shouldContainKeys
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeout
import uk.dioxic.helios.execute.model.ExecutionContext
import uk.dioxic.helios.execute.model.InsertOneExecutor
import uk.dioxic.helios.execute.model.MessageExecutor
import uk.dioxic.helios.execute.model.TpsRate
import uk.dioxic.helios.execute.resources.ResourceRegistry
import uk.dioxic.helios.execute.results.*
import uk.dioxic.helios.execute.test.IS_NOT_GH_ACTION
import uk.dioxic.helios.generate.buildTemplate
import uk.dioxic.helios.generate.operators.ChooseOperator
import uk.dioxic.helios.generate.operators.NameOperator
import uk.dioxic.helios.generate.operators.VarOperator
import uk.dioxic.helios.generate.putKeyedOperator
import uk.dioxic.helios.generate.putOperator
import uk.dioxic.helios.generate.putOperatorObject
import kotlin.time.Duration
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
                    rateWorkload(executor = executor, count = 5)
                    rateWorkload(executor = executor, count = 5)
                }
            }.execute().count()

            verifyExCount(10)
        }


        test("parallel stage with rate workloads has correct execution count") {

            buildBenchmark {
                parallelStage {
                    rateWorkload(executor = executor, count = 5)
                    rateWorkload(executor = executor, count = 5)
                }
            }.execute().count()

            verifyExCount(10)
        }

        test("parallel stage with weighted workloads has correct execution count") {
            buildBenchmark {
                parallelStage {
                    weightedWorkload(executor = executor, count = 5)
                    weightedWorkload(executor = executor, count = 5)
                }
            }.execute().count()

            verifyExCount(10)
        }

        test("parallel stage with mixed workloads has correct execution count") {
            buildBenchmark {
                parallelStage {
                    weightedWorkload(executor = executor, count = 5)
                    weightedWorkload(executor = executor, count = 5)
                    rateWorkload(executor = executor, count = 5)
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
                    rateWorkload(executor = executor, count = count.toLong(), rate = TpsRate(tps))
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
                    rateWorkload(executor = executor, count = count.toLong(), rate = TpsRate(tps))
                    rateWorkload(executor = executor, count = count.toLong(), rate = TpsRate(tps))
                    rateWorkload(executor = executor, count = count.toLong(), rate = TpsRate(tps))
                    rateWorkload(executor = executor, count = count.toLong(), rate = TpsRate(tps))
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
                rateWorkload(executor = executor, count = 1)
            }
        }.execute().collect {
            if (it is ProgressMessage) {
                it.result.shouldBeInstanceOf<TimedResult>()
            }
        }
    }

    test("multiple executions get summarized") {
        buildBenchmark {
            parallelStage {
                rateWorkload(executor = executor, count = 100)
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
                parallelStage(timeout = timeout) {
                    rateWorkload(executor = executor, count = 1_000_000, rate = TpsRate(10))
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

        buildBenchmark {
            parallelStage {
                rateWorkload(executor = insertOneExecutor, count = 100, rate = TpsRate(50))
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
                rateWorkload(executor = executor, count = 100, rate = TpsRate(100))
                rateWorkload(executor = executor, count = 100, rate = TpsRate(100))
            }
        }.execute().collect {
            println(it)
        }
    }

    test("variables are sync'd across workloads") {
        val stageVariables = buildTemplate {
            putOperatorObject<ChooseOperator>("cities") {
                putBsonArray("from") {
                    addAll(listOf("Belfast", "London", "Madrid"))
                }
            }
            putOperator<NameOperator>("name")
        }
        val workloadVariables1 = buildTemplate {
            put("species", "bird")
        }
        val workloadVariables2 = buildTemplate {
            put("species", "mammal")
        }
        val template = buildTemplate {
            putKeyedOperator<VarOperator>("name", "name")
            putKeyedOperator<VarOperator>("species", "species")
            putKeyedOperator<VarOperator>("cities", "cities")
        }
        val benchmark = buildBenchmark {
            parallelStage(variables = stageVariables) {
                rateWorkload(
                    variables = workloadVariables1,
                    executor = MessageExecutor(template),
                    count = 10
                )
                rateWorkload(
                    variables = workloadVariables2,
                    executor = MessageExecutor(template),
                    count = 10
                )
            }
        }
        benchmark.execute(interval = Duration.ZERO)
            .filter { it is ProgressMessage }
            .map { (it as ProgressMessage).result }
            .filter { it is TimedMessageResult }
            .map { it as TimedMessageResult }
            .map { it.value }
            .onEach { it.doc.shouldContainKeys("name", "cities", "species") }
            .map { it.doc.filter { (k, _) -> k != "species" } }
            .toList()
            .distinct().should { d ->
                d.count() shouldBe 10
                d.forEach {
                    println(it)
                }
            }
    }

})