package uk.dioxic.mgenerate.worker

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveAtMostSize
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import uk.dioxic.mgenerate.worker.results.OutputResult
import uk.dioxic.mgenerate.worker.results.SummarizedMessageResult
import uk.dioxic.mgenerate.worker.results.SummarizedResult
import uk.dioxic.mgenerate.worker.results.TimedMessageResult
import java.time.LocalTime
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class WorkerTests : FunSpec({

    test("single stage workload") {
        val stageName = "single"
        val workloadName = "workload"
        val stage = SingleStage(
            name = stageName,
            workload = CommandWorkload(name = workloadName)
        )

        executeStages(stage, tick = 500.milliseconds).collect {
            it.shouldBeInstanceOf<TimedMessageResult>()
            it.workloadName shouldBe workloadName
            it.value.msg shouldBe "hello"
            it.duration shouldBeLessThan 100.milliseconds
            println(it)
        }
    }

    test("workload rate") {
        val stage = MultiExecutionStage(
            name = "testStage",
            workloads = listOf(
                MessageWorkload(name = "workload1", count = 1_000, rate = 1000.tps) { "[$it] hello1" },
            )
        )

        val results = mutableListOf<OutputResult>()
        executeStages(stage, tick = 100.milliseconds)
            .toList(results)
            .shouldBeInstanceOf<List<SummarizedMessageResult>>()
            .should { list ->
                list.forEach {
                    println("${LocalTime.now()} - $it")
                    it.workloadName shouldStartWith "workload"
                }
                list.sumOf { it.msgCount } shouldBe 1000
                list.map { it.msgCount }.average()
                    .shouldBeGreaterThan(90.0)
                    .shouldBeLessThan(115.0)
            }


        println("done")
    }

    test("coroutine context") {
        val stages = arrayOf(
            SingleStage(
                "singleStage",
                workload = CommandWorkload(name = "wk1")
            ), MultiExecutionStage(
                name = "testStage",
                timeout = 5.seconds,
                rate = Rate.of(1),
                workloads = listOf(
                    MessageWorkload(name = "wk1", count = 5) { "[$it] hello1" },
                ),
            )
        )

        executeStages(*stages, tick = 1.seconds).collect {
            println("${LocalTime.now()} - $it")
        }
    }
})