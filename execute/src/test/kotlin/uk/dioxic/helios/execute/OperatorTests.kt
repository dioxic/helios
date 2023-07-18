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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.bson.Bson
import kotlinx.serialization.encodeToString
import org.bson.types.ObjectId
import uk.dioxic.helios.execute.model.Benchmark
import uk.dioxic.helios.execute.model.MessageExecutor
import uk.dioxic.helios.execute.model.PeriodRate
import uk.dioxic.helios.execute.results.TimedMessageResult
import uk.dioxic.helios.generate.*
import uk.dioxic.helios.generate.operators.ConstOperator
import uk.dioxic.helios.generate.operators.NameOperator
import uk.dioxic.helios.generate.operators.ObjectIdOperator
import uk.dioxic.helios.generate.operators.VarOperator
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class OperatorTests : FunSpec({

    val bson = Bson {
        prettyPrint = true
    }

    context("constants") {
        context("lookups") {

            val template = buildTemplate {
                putKeyedOperator<ConstOperator>("name", "animal")
            }
            val executor = MessageExecutor(template)
            val consts = buildTemplate {
                put("animal", "halibut")
            }

            println(bson.encodeToString(template))

            suspend fun Benchmark.verify() {
                execute().filterIsInstance<ProgressMessage>().collect { msg ->
                    msg.result.shouldBeInstanceOf<TimedMessageResult>().value.doc.should { doc ->
                        doc["name"] shouldBe "halibut"
                    }
                }
            }

            test("benchmark lookup is successful") {
                buildBenchmark(constants = consts) {
                    sequentialStage {
                        rateWorkload(executor = executor)
                    }
                }.verify()
            }

            test("stage lookup is successful") {
                buildBenchmark {
                    sequentialStage(constants = consts) {
                        rateWorkload(executor = executor)
                    }
                }.verify()
            }

            test("workload lookup is successful") {
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
        val variables = buildTemplate {
            putOperator<ObjectIdOperator>("oid")
        }
        val template = buildTemplate {
            putKeyedOperator<VarOperator>("myId", "oid")
        }
        val executor = MessageExecutor(template)

        suspend fun Benchmark.verify(distinctCount: Int) {
            execute(
                interval = Duration.ZERO,
            ).filterIsInstance<ProgressMessage>()
                .map { it.result }
                .filterIsInstance<TimedMessageResult>()
                .map { it.value }
                .toList().should { msgs ->
                    msgs.forEach {
//                        println(it.doc)
                        it.doc["myId"].shouldBeInstanceOf<ObjectId>()
                    }
                    msgs.distinctBy { it.doc["myId"] }.count() shouldBe distinctCount
                }
        }
        context("unlinked") {

            test("benchmark lookup is successful") {
                buildBenchmark(variables = variables) {
                    sequentialStage {
                        rateWorkload(executor = executor, count = 4)
                        rateWorkload(executor = executor, count = 2)
                    }
                }.verify(6)
            }


            test("stage lookup is successful") {
                buildBenchmark {
                    parallelStage(variables = variables) {
                        rateWorkload(executor = executor, count = 4)
                        rateWorkload(executor = executor, count = 8)
                    }
                }.verify(12)
            }

            test("workload lookup is successful") {
                buildBenchmark {
                    parallelStage {
                        rateWorkload(variables = variables, executor = executor, count = 4)
                        rateWorkload(variables = variables, executor = executor, count = 2)
                        rateWorkload(variables = variables, executor = executor, count = 4)
                    }
                }.verify(10)
            }
        }

        context("linked variables") {
            test("benchmark variables linked") {
                buildBenchmark(variables = variables) {
                    parallelStage(sync = true) {
                        rateWorkload(executor = executor, count = 4, rate = PeriodRate(100.milliseconds))
                        rateWorkload(executor = executor, count = 8)
                        rateWorkload(executor = executor, count = 4)
                    }
                }.verify(8)
            }

            test("stage variables linked") {
                buildBenchmark {
                    parallelStage(variables = variables, sync = true) {
                        rateWorkload(executor = executor, count = 2, rate = PeriodRate(300.milliseconds))
                        rateWorkload(executor = executor, count = 5, rate = PeriodRate(100.milliseconds))
                        rateWorkload(executor = executor, count = 1)
                    }
                }.verify(5)
            }
        }

        test("only call lazy variables if necessary") {
            mockkStatic("uk.dioxic.helios.generate.HydrationKt")

            val templateNoLookups = buildTemplate {
                putOperator<NameOperator>("name")
            }

            every {
                with(any<OperatorContext>()) { variables.hydrate() }
            } returns emptyMap<String, Any?>()


            buildBenchmark(variables = variables) {
                sequentialStage {
                    rateWorkload(executor = MessageExecutor(templateNoLookups))
                }
            }.execute().collect()

            verify(inverse = true) {
                with(any<OperatorContext>()) { variables.hydrate() }
            }
        }
    }

})