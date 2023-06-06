package uk.dioxic.mgenerate.worker

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import uk.dioxic.mgenerate.worker.results.OutputResult
import uk.dioxic.mgenerate.worker.results.SummarizedMessageResult
import uk.dioxic.mgenerate.worker.results.TimedMessageResult
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class)
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

    test("workload summarization count") {
        val stage = MultiExecutionStage(
            name = "testStage",
            workloads = listOf(
                MessageWorkload(name = "workload1000", count = 1_000) { "[$it] hello1" },
                MessageWorkload(name = "workload1000R", count = 1_000, rate = 500.tps) { "[$it] hello1" },
                MessageWorkload(name = "workload500", count = 500) { "[$it] hello1" },
                MessageWorkload(name = "workload200", count = 200) { "[$it] hello1" },
            )
        )

        var count = 0
        executeStages(stage, tick = 100.milliseconds).collect {
            println(it)
            it.shouldBeInstanceOf<SummarizedMessageResult>()
            count += it.msgCount
        }

        count shouldBe stage.workloads.sumOf { it.count }
    }

    test("workload tick rate") {
        val stage = MultiExecutionStage(
            name = "testStage",
            workloads = listOf(
                MessageWorkload(name = "workload500", count = 500, rate = 500.tps) { "[$it] hello1" },
            )
        )

        var lastTick: TimeMark? = null
        executeStages(stage, tick = 100.milliseconds).collect {
            lastTick?.elapsedNow()?.also {
                println(it)
                it.shouldBeGreaterThanOrEqualTo(100.milliseconds)
                it.shouldBeLessThan(120.milliseconds)
            }
            lastTick = TimeSource.Monotonic.markNow()
        }
    }


    test("workload rate") {
        val stage = MultiExecutionStage(
            name = "testStage",
            workloads = listOf(
                MessageWorkload(name = "workload1000", count = 1_000, rate = 1000.tps) { "[$it] hello1" },
                MessageWorkload(name = "workload500", count = 1_000, rate = 500.tps) { "[$it] hello1" },
            )
        )

        val results = mutableListOf<OutputResult>()
        executeStages(stage, tick = 100.milliseconds).collect {
            println(it)
            results.add(it)
        }

        results
            .shouldBeInstanceOf<List<SummarizedMessageResult>>()
            .should { list ->
                list.forEach {
                    it.workloadName shouldStartWith "workload"
                }
//                list.sumOf { it.msgCount } shouldBe stage.workloads.sumOf { it.count }
                list.filter { it.workloadName == "workload1000" }
                    .map { it.msgCount }.average()
                    .shouldBeGreaterThan(90.0)
                    .shouldBeLessThan(115.0)

                list.filter { it.workloadName == "workload500" }
                    .map { it.msgCount }.average()
                    .shouldBeGreaterThan(40.0)
                    .shouldBeLessThan(60.0)
            }
    }

    test("workload timeout") {
        val stages = arrayOf(
            SingleStage(
                "singleStage",
                workload = CommandWorkload(name = "wk1")
            ), MultiExecutionStage(
                name = "testStage",
                timeout = 1.seconds,
                rate = Rate.of(1),
                workloads = listOf(
                    MessageWorkload(name = "wk1", count = 5) { "[$it] hello1" },
                ),
            )
        )

        measureTime {
            executeStages(*stages, tick = 100.milliseconds).collect {
                println(it)
            }
        }.shouldBeGreaterThanOrEqualTo(1.seconds)
    }
})