package uk.dioxic.mgenerate.execute

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.withTimeout
import uk.dioxic.mgenerate.execute.model.MessageExecutor
import uk.dioxic.mgenerate.execute.model.TpsRate
import uk.dioxic.mgenerate.execute.results.MessageResult
import uk.dioxic.mgenerate.execute.results.SummarizedResultsBatch
import uk.dioxic.mgenerate.execute.results.TimedResult
import uk.dioxic.mgenerate.execute.test.IS_NOT_GH_ACTION
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class)
class FrameworkTest : FunSpec({

    val executor = mockk<MessageExecutor>()

    afterTest {
        clearMocks(executor)
    }

    test("sequential stage has correct execution count") {
        every { executor.execute(any(), any()) } returns MessageResult("hello world!")

        buildBenchmark {
            sequentialStage {
                rateWorkload(executor = executor, count = 5)
                rateWorkload(executor = executor, count = 5)
            }
        }.execute().count()

        verify(exactly = 10) { executor.execute(any(), any()) }
    }

    test("parallel stage with rate workloads has correct execution count") {
        every { executor.execute(any(), any()) } returns MessageResult("hello world!")
        buildBenchmark {
            parallelStage {
                rateWorkload(executor = executor, count = 5)
                rateWorkload(executor = executor, count = 5)
            }
        }.execute().count()

        verify(exactly = 10) { executor.execute(any(), any()) }
    }

    test("parallel stage with weighted workloads has correct execution count") {
        every { executor.execute(any(), any()) } returns MessageResult("hello world!")

        buildBenchmark {
            parallelStage {
                weightedWorkload(executor = executor, count = 5)
                weightedWorkload(executor = executor, count = 5)
            }
        }.execute().count()

        verify(exactly = 10) { executor.execute(any(), any()) }
    }

    test("parallel stage with mixed workloads has correct execution count") {
        every { executor.execute(any(), any()) } returns MessageResult("hello world!")

        buildBenchmark {
            parallelStage {
                weightedWorkload(executor = executor, count = 5)
                weightedWorkload(executor = executor, count = 5)
                rateWorkload(executor = executor, count = 5)
            }
        }.execute().count()

        verify(exactly = 15) { executor.execute(any(), any()) }
    }

    test("workload tps rate is roughly correct for single execution").config(enabled = IS_NOT_GH_ACTION) {
        every { executor.execute(any(), any()) } returns MessageResult("hello world!")

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
        every { executor.execute(any(), any()) } returns MessageResult("hello world!")

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
        every { executor.execute(any(), any()) } returns MessageResult("hello world!")

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
        every { executor.execute(any(), any()) } returns MessageResult("hello world!")

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
        every { executor.execute(any(), any()) } returns MessageResult("hello world!")
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

})