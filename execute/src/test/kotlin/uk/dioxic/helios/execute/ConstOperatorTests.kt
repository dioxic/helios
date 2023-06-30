package uk.dioxic.helios.execute

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.serialization.json.put
import uk.dioxic.helios.execute.model.Benchmark
import uk.dioxic.helios.execute.model.MessageExecutor
import uk.dioxic.helios.execute.operators.ConstOperator
import uk.dioxic.helios.execute.results.TimedMessageResult
import uk.dioxic.helios.generate.*
import uk.dioxic.helios.generate.operators.NameOperator

class ConstOperatorTests : FunSpec({

    OperatorFactory.addOperator(ConstOperator::class)


    context("lookups") {

        val template = buildTemplate {
            putKeyedOperator<ConstOperator>("name", "animal")
        }
        val executor = MessageExecutor(template)
        val consts = buildTemplate {
            put("animal", "halibut")
        }

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
    }

    test("only call lazy constants if necessary") {
        mockkStatic("uk.dioxic.helios.generate.HydrationKt")

        val template = buildTemplate {
            putOperator<NameOperator>("name")
        }
        val executor = MessageExecutor(template)
        val consts = buildTemplate {
            put("animal", "halibut")
        }

        every {
            with(any<OperatorContext>()) { consts.hydrate() }
        } returns emptyMap<String, Any?>()


        buildBenchmark(constants = consts) {
            sequentialStage {
                rateWorkload(executor = executor)
            }
        }.execute().collect()

        verify(inverse = true) {
            with(any<OperatorContext>()) { consts.hydrate() }
        }
    }

})