package uk.dioxic.helios.execute

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.put
import org.bson.types.ObjectId
import uk.dioxic.helios.execute.model.Benchmark
import uk.dioxic.helios.execute.model.MessageExecutor
import uk.dioxic.helios.execute.operators.ConstOperator
import uk.dioxic.helios.execute.operators.VarOperator
import uk.dioxic.helios.execute.results.TimedMessageResult
import uk.dioxic.helios.generate.*
import uk.dioxic.helios.generate.operators.NameOperator
import uk.dioxic.helios.generate.operators.ObjectIdOperator
import kotlin.time.Duration

class OperatorTests : FunSpec({

    OperatorFactory.addOperator(ConstOperator::class)
    OperatorFactory.addOperator(VarOperator::class)

    context("constants") {
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
    }

    context("variables") {
        context("lookups") {
            val template = buildTemplate {
                putKeyedOperator<VarOperator>("myId", "oid")
            }
            val executor = MessageExecutor(template)
            val variables = buildTemplate {
                putOperator<ObjectIdOperator>("oid")
            }

            suspend fun Benchmark.verify() {
                execute(interval = Duration.ZERO).filterIsInstance<ProgressMessage>()
                    .map { it.result }
                    .filterIsInstance<TimedMessageResult>()
                    .map { it.value }
                    .onEach {
                        it.doc["myId"].shouldBeInstanceOf<ObjectId>()
                    }.distinctUntilChanged().count() shouldBe 4
            }

            test("benchmark constant lookup is successful") {
                buildBenchmark(variables = variables) {
                    sequentialStage {
                        rateWorkload(executor = executor, count = 4)
                    }
                }.verify()
            }

            test("stage constant lookup is successful") {
                buildBenchmark {
                    sequentialStage(variables = variables) {
                        rateWorkload(executor = executor, count = 4)
                    }
                }.verify()
            }

            test("workload constant lookup is successful") {
                buildBenchmark {
                    sequentialStage {
                        rateWorkload(variables = variables, executor = executor, count = 4)
                    }
                }.verify()
            }
        }

        test("only call lazy variables if necessary") {
            mockkStatic("uk.dioxic.helios.generate.HydrationKt")

            val template = buildTemplate {
                putOperator<NameOperator>("name")
            }
            val executor = MessageExecutor(template)
            val variables = buildTemplate {
                put("animal", "halibut")
            }

            every {
                with(any<OperatorContext>()) { variables.hydrate() }
            } returns emptyMap<String, Any?>()


            buildBenchmark(variables = variables) {
                sequentialStage {
                    rateWorkload(executor = executor)
                }
            }.execute().collect()

            verify(inverse = true) {
                with(any<OperatorContext>()) { variables.hydrate() }
            }
        }
    }

})