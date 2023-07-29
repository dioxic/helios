package uk.dioxic.helios.execute

import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeBetween
import io.kotest.matchers.maps.shouldContainKeys
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId
import uk.dioxic.helios.execute.model.Benchmark
import uk.dioxic.helios.execute.model.MessageExecutor
import uk.dioxic.helios.execute.model.PeriodRate
import uk.dioxic.helios.execute.model.StreamDictionary
import uk.dioxic.helios.execute.test.mapMessageResults
import uk.dioxic.helios.generate.buildTemplate
import uk.dioxic.helios.generate.operators.*
import uk.dioxic.helios.generate.putKeyedOperator
import uk.dioxic.helios.generate.putOperator
import uk.dioxic.helios.generate.putOperatorObject
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds

class VariableTests : FunSpec({

    val defaultVariables = buildTemplate {
        putOperator<ObjectIdOperator>("oid")
    }
    val defaultTemplate = buildTemplate {
        putKeyedOperator<VarOperator>("myId", "oid")
    }
    val defaultExecutor = MessageExecutor(defaultTemplate)

    suspend fun Benchmark.verify(distinctCount: Int) {
        execute(
            interval = ZERO,
        ).filterIsInstance<ProgressMessage>()
            .mapMessageResults()
            .toList().should { msgs ->
                msgs.forEach {
                    it.doc["myId"].shouldBeInstanceOf<ObjectId>()
                }
                msgs.distinctBy { it.doc["myId"] }.count() shouldBe distinctCount
            }
    }

    test("benchmark variables are linked") {
        buildBenchmark {
            variables = defaultVariables
            parallelStage {
                sync = true
                addRateWorkload(executor = defaultExecutor, count = 4, rate = PeriodRate(100.milliseconds))
                addRateWorkload(executor = defaultExecutor, count = 8)
                addRateWorkload(executor = defaultExecutor, count = 4)
            }
        }.verify(8)
    }

    test("stage variables are linked") {
        buildBenchmark {
            parallelStage {
                variables = defaultVariables
                sync = true
                addRateWorkload(executor = defaultExecutor, count = 2, rate = PeriodRate(300.milliseconds))
                addRateWorkload(executor = defaultExecutor, count = 5, rate = PeriodRate(100.milliseconds))
                addRateWorkload(executor = defaultExecutor, count = 1)
            }
        }.verify(5)
    }

    test("workload variables are unlinked") {
        buildBenchmark {
            parallelStage {
                addRateWorkload(variables = defaultVariables, executor = defaultExecutor, count = 4)
                addRateWorkload(variables = defaultVariables, executor = defaultExecutor, count = 2)
                addRateWorkload(variables = defaultVariables, executor = defaultExecutor, count = 4)
            }
        }.verify(10)
    }

    test("variables can access dictionaries") {
        buildBenchmark {
            sequentialStage {
                dictionaries = mapOf(
                    "person" to StreamDictionary(buildTemplate {
                        putOperator<NameOperator>("name")
                        putOperatorObject<IntOperator>("age") {
                            put("max", 5)
                        }
                    })
                )
                variables = buildTemplate {
                    putKeyedOperator<DictionaryOperator>("varPerson", "person")
                }
                addRateWorkload(
                    count = 5,
                    executor = MessageExecutor(buildTemplate {
                        putKeyedOperator<VarOperator>("myName", "varPerson.name")
                        putKeyedOperator<VarOperator>("myAge", "varPerson.age")
                    })
                )
            }
        }.execute(interval = ZERO).mapMessageResults().toList().should { results ->
            results shouldHaveSize 5
            results.map { it.doc }.forAll { doc ->
                doc.shouldContainKeys("myName", "myAge")
                doc["myAge"].shouldBeInstanceOf<Int>().shouldBeBetween(0, 5)
            }
        }
    }

})