package uk.dioxic.mgenerate.execute.report

import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearMocks
import io.mockk.mockk
import uk.dioxic.mgenerate.execute.buildBenchmark
import uk.dioxic.mgenerate.execute.defaultExecutor
import uk.dioxic.mgenerate.execute.execute
import uk.dioxic.mgenerate.execute.model.MessageExecutor
import uk.dioxic.mgenerate.execute.model.TpsRate
import kotlin.time.Duration.Companion.seconds

class ReportFormattingTest : FunSpec({

    val executor = mockk<MessageExecutor>()

    afterTest {
        clearMocks(executor)
    }

    test("print multiple workloads") {
        runBlocking {
            buildBenchmark {
                parallelStage {
                    rateWorkload(executor = defaultExecutor, count = 100, rate = TpsRate(15))
                    rateWorkload(executor = defaultExecutor, count = 100, rate = TpsRate(50))
                    rateWorkload(executor = defaultExecutor, count = 100, rate = TpsRate(60))
                }
            }.execute().format(ReportFormatter.create(ReportFormat.TEXT)).collect {
                print(it)
            }
        }
    }

    test("scratch") {

        val wpr = WorkloadProgressReport(
            workloadName = "workload1",
            insertedCount = 100,
            progress = 50,
            operationCount = 200,
            elapsed = 4.seconds
        ).toMap()

        println(wpr)

    }


})