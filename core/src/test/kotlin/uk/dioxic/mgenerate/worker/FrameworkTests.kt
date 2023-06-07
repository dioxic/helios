package uk.dioxic.mgenerate.worker

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bson.Document
import uk.dioxic.mgenerate.test.IS_NOT_GH_ACTION
import uk.dioxic.mgenerate.worker.results.OutputResult
import uk.dioxic.mgenerate.worker.results.SummarizedMessageResult
import uk.dioxic.mgenerate.worker.results.TimedCommandResult
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class)
class FrameworkTests : FunSpec({

    test("single stage workload") {
        val client = mockk<MongoClient>()
        val database = mockk<MongoDatabase>()
        val helloCommand = Document("hello", 1)

        every { client.getDatabase(any()) } returns database
        every { database.runCommand(any()) } returns Document("ok", 1)

        val workloadName = "workload"
        val stage = SingleStage(
            name = "single",
            workload = SingleExecutionWorkload(workloadName, CommandExecutor(
                client = client,
                command = helloCommand,
                database = "test"
            ))
        )

        executeStages(stage, tick = 500.milliseconds).collect {
            it.shouldBeInstanceOf<TimedCommandResult>()
            it.workloadName shouldBe workloadName
            it.value.success shouldBe true
            it.duration shouldBeLessThan 100.milliseconds
            println(it)
        }

        verify { client.getDatabase("test") }
        verify { database.runCommand(any()) }
    }

    test("workload summarization count") {
        val executor = MessageExecutor { "[$it] hello" }

        val stage = MultiExecutionStage(
            name = "testStage",
            workloads = listOf(
                MultiExecutionWorkload(name = "workload1000", count = 1_000, executor = executor),
                MultiExecutionWorkload(name = "workload1000R", count = 1_000, rate = 500.tps, executor = executor),
                MultiExecutionWorkload(name = "workload500", count = 500, executor = executor),
                MultiExecutionWorkload(name = "workload200", count = 200, executor = executor),
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
                MultiExecutionWorkload(
                    name = "workload500",
                    count = 500,
                    rate = 500.tps,
                    executor = MessageExecutor { "[$it] hello1" }
                ),
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


    test("workload rate").config(enabled = IS_NOT_GH_ACTION) {
        val executor = MessageExecutor { "[$it] hello" }
        val stage = MultiExecutionStage(
            name = "testStage",
            workloads = listOf(
                MultiExecutionWorkload(name = "workload1000", count = 1_000, rate = 1000.tps, executor = executor),
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
                list.filter { it.workloadName == "workload1000" }
                    .map { it.msgCount }.average()
                    .shouldBeGreaterThan(90.0)
                    .shouldBeLessThan(120.0)
            }
    }

    test("workload timeout") {
        val stages = arrayOf(
            SingleStage(
                "singleStage",
                workload = SingleExecutionWorkload("single", MessageExecutor { "[$it] hello" })
            ), MultiExecutionStage(
                name = "testStage",
                timeout = 1.seconds,
                rate = Rate.of(1),
                workloads = listOf(
                    MultiExecutionWorkload(
                        name = "wk1",
                        count = 5,
                        executor = MessageExecutor { "[$it] hello1" }
                    )
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