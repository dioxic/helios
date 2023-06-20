package uk.dioxic.mgenerate.execute

import io.kotest.core.spec.style.FunSpec
import uk.dioxic.mgenerate.execute.model.PeriodRate
import uk.dioxic.mgenerate.execute.model.StageCompleteMessage
import uk.dioxic.mgenerate.execute.model.StageStartMessage
import uk.dioxic.mgenerate.execute.model.WorkloadProgressMessage
import uk.dioxic.mgenerate.execute.resources.ResourceRegistry
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