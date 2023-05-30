package uk.dioxic.mgenerate.worker

import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import uk.dioxic.mgenerate.worker.results.GetSummarizedResults
import uk.dioxic.mgenerate.worker.results.SummarizedResult
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class WorkerTests : FunSpec({

    test("basic") {
        val workloads = listOf(
            MessageWorkload(name = "wk1") { "[$it] hello" },
            MessageWorkload(name = "wk2") { "[$it] hello" },
            MessageWorkload(name = "wk3") { "[$it] hello" },
        )

        runBlocking {
//            withTimeout(5.seconds) {
            val resultChannel = executeWorkloads(workloads)

            launch {
                repeat(10) {
                    delay(100.milliseconds)
                    val response = CompletableDeferred<Map<String, SummarizedResult>>()
                    if (resultChannel.isClosedForSend) {
                        cancel()
                    }
                    resultChannel.send(GetSummarizedResults(response))
                    println(response.await())
                }
            }
//            }
        }
        println("done")
    }
})