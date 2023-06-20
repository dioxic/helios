package uk.dioxic.mgenerate.worker

import io.kotest.core.spec.style.FunSpec
import uk.dioxic.mgenerate.resources.ResourceRegistry
import uk.dioxic.mgenerate.test.defaultExecutor
import uk.dioxic.mgenerate.worker.model.PeriodRate
import uk.dioxic.mgenerate.worker.model.StageCompleteMessage
import uk.dioxic.mgenerate.worker.model.StageStartMessage
import uk.dioxic.mgenerate.worker.model.WorkloadProgressMessage
import kotlin.time.Duration.Companion.seconds

class FrameworkTest : FunSpec({

    test("sequential stage") {
        val benchmark = buildBenchmark {
            sequentialStage {
                rateWorkload(
                    executor = defaultExecutor,
                    rate = PeriodRate(1.seconds, 0.5),
                    count = 5,
                )
                rateWorkload(
                    executor = defaultExecutor,
                    count = 5,
                )
            }
        }

        val registry = ResourceRegistry()

        executeBenchmark(benchmark, registry).collect {
            println(when(it) {
                is StageStartMessage -> "stage start"
                is StageCompleteMessage -> "stage complete"
                is WorkloadProgressMessage -> it.result.value
                else -> error("unexpected message")
            })
        }

    }

})