package uk.dioxic.helios.execute

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.serialization.json.put
import uk.dioxic.helios.execute.model.Benchmark
import uk.dioxic.helios.execute.model.MessageExecutor
import uk.dioxic.helios.execute.operators.ConstOperator
import uk.dioxic.helios.execute.results.TimedMessageResult
import uk.dioxic.helios.generate.OperatorFactory
import uk.dioxic.helios.generate.buildTemplate
import uk.dioxic.helios.generate.putKeyedOperator

class ConstOperatorTests : FunSpec({

    OperatorFactory.addOperator(ConstOperator::class)

    val template = buildTemplate {
        putKeyedOperator<ConstOperator>("name", "animal")
    }
    val consts = buildTemplate {
        put("animal", "halibut")
    }
    val executor = MessageExecutor(template)

    suspend fun Benchmark.verify() {
        execute().filterIsInstance<ProgressMessage>().collect { msg ->
            msg.result.shouldBeInstanceOf<TimedMessageResult>().value.doc.should { doc ->
                doc["name"] shouldBe "halibut"
            }
        }
    }

    test("benchmark constant lookup is successful") {
        buildBenchmark(constants = consts) {
            sequentialStage {
                rateWorkload(executor = executor)
            }
        }.verify()
    }

    test("stage constant lookup is successful") {
        buildBenchmark {
            sequentialStage(constants = consts) {
                rateWorkload(executor = executor)
            }
        }.verify()
    }

    test("workload constant lookup is successful") {
        buildBenchmark {
            sequentialStage {
                rateWorkload(constants = consts, executor = executor)
            }
        }.verify()
    }

})