package uk.dioxic.helios.execute

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId
import uk.dioxic.helios.execute.model.Benchmark
import uk.dioxic.helios.execute.model.MessageExecutor
import uk.dioxic.helios.execute.model.PeriodRate
import uk.dioxic.helios.execute.results.TimedMessageResult
import uk.dioxic.helios.generate.buildTemplate
import uk.dioxic.helios.generate.operators.ObjectIdOperator
import uk.dioxic.helios.generate.operators.VarOperator
import uk.dioxic.helios.generate.putKeyedOperator
import uk.dioxic.helios.generate.putOperator
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds

class VariableTests : FunSpec({

    val variables = buildTemplate {
        putOperator<ObjectIdOperator>("oid")
    }
    val template = buildTemplate {
        putKeyedOperator<VarOperator>("myId", "oid")
    }
    val executor = MessageExecutor(template)

    suspend fun Benchmark.verify(distinctCount: Int) {
        execute(
            interval = ZERO,
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

    test("benchmark variables linked") {
        buildBenchmark {
            this.variables = variables
            parallelStage {
                sync = true
                addRateWorkload(executor = executor, count = 4, rate = PeriodRate(100.milliseconds))
                addRateWorkload(executor = executor, count = 8)
                addRateWorkload(executor = executor, count = 4)
            }
        }.verify(8)
    }

    test("stage variables linked") {
        buildBenchmark {
            parallelStage {
                this.variables = variables
                sync = true
                addRateWorkload(executor = executor, count = 2, rate = PeriodRate(300.milliseconds))
                addRateWorkload(executor = executor, count = 5, rate = PeriodRate(100.milliseconds))
                addRateWorkload(executor = executor, count = 1)
            }
        }.verify(5)
    }

    test("workload variables are unlinked") {
        buildBenchmark {
            parallelStage {
                addRateWorkload(variables = variables, executor = executor, count = 4)
                addRateWorkload(variables = variables, executor = executor, count = 2)
                addRateWorkload(variables = variables, executor = executor, count = 4)
            }
        }.verify(10)
    }

})