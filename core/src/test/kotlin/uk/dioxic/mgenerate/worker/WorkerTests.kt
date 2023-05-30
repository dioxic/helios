package uk.dioxic.mgenerate.worker

import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.*
import java.time.LocalTime
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class WorkerTests : FunSpec({

    test("basic") {
        val stages = arrayOf(
            SingleStage(
                "singleStage",
                workload = CommandWorkload(name = "wk1")
            ),
            MultiExecutionStage(
                name = "testStage",
                timeout = 5.seconds,
                workloads = listOf(
                    MessageWorkload(name = "wk1", count = 1_000_000) { "[$it] hello1" },
                    MessageWorkload(name = "wk2", count = 1_000_000) { "[$it] hello2" },
                    MessageWorkload(name = "wk3", count = 1_000_000) { "[$it] hello3" },
                    MessageWorkload(name = "wk4", count = Long.MAX_VALUE) { "[$it] hello4" },
                )
            )
        )

        runBlocking(Dispatchers.Default) {
            executeStages(*stages, tick = 500.milliseconds).collect {
                println("${LocalTime.now()} - $it")
            }
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

        runBlocking {
            executeStages(*stages, tick = 1.seconds).collect {
                println("${LocalTime.now()} - $it")
            }
        }
        println("${LocalTime.now()} done")
    }
})