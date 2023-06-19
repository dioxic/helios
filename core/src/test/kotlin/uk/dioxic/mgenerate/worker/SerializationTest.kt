package uk.dioxic.mgenerate.worker

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.put
import uk.dioxic.mgenerate.buildTemplate
import uk.dioxic.mgenerate.operators.Operator
import uk.dioxic.mgenerate.test.readResource
import uk.dioxic.mgenerate.worker.model.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SerializationTest : FunSpec({
    val json = Json {
        prettyPrint = true
    }

    test("can handle 64-bit integer") {
        val benchmark = buildBenchmark {
            sequentialStage {
                rateWorkload(
                    count = Long.MAX_VALUE
                )
            }
        }

        val str = json.encodeToString(benchmark)
        println(str)
        val decoded = json.decodeFromString<Benchmark>(str)
        decoded.stages[0].workloads.first().count shouldBeExactly Long.MAX_VALUE
    }

    test("command executor") {
        val benchmark = buildBenchmark {
            sequentialStage {
                rateWorkload(
                    executor = CommandExecutor("myDB", buildTemplate { put("buildInfo", 1) })
                )
            }
        }
        val str = json.encodeToString(benchmark)
        println(str)
        val decoded = json.decodeFromString<Benchmark>(str)
        decoded.stages[0].workloads.first().executor.shouldBeInstanceOf<CommandExecutor>()
    }

    test("polymorphic deserialization works without type") {
        val str = """
        {
            "type": "sequential",
            "name": "stage0",
            "workloads": [
                {
                    "name": "workload0",
                    "executor": {
                        "type": "insertOne",
                        "database": "myDB",
                        "collection": "myCollection",
                        "template": {
                            "name": "Bob"
                        }
                    },
                    "weight": 1
                },{
                    "name": "workload1",
                    "executor": {
                        "type": "insertOne",
                        "database": "myDB",
                        "collection": "myCollection",
                        "template": {
                            "name": "Bob"
                        }
                    },
                    "rate": {
                        "tps": 100
                    }
                }
            ]
        }
        """.trimIndent()

        val stage = json.decodeFromString<Stage>(str)

        stage.shouldBeInstanceOf<SequentialStage>()
            .workloads.should {
                it.shouldHaveSize(2)
                it[0].shouldBeInstanceOf<WeightedWorkload>()
                it[1].shouldBeInstanceOf<RateWorkload>()
                    .rate.shouldBeInstanceOf<TpsRate>()
            }

        println(json.encodeToString(stage))

    }

    test("basic") {
        val benchmark = buildBenchmark {
            sequentialStage {
                rateWorkload()
            }
            parallelStage(timeout = 5.milliseconds) {
                rateWorkload(
                    rate = TpsRate(tps = 500),
                )
                rateWorkload(
                    rate = UnlimitedRate
                )
                rateWorkload(
                    rate = RampedRate(
                        from = PeriodRate(period = 1.seconds),
                        to = TpsRate(tps = 9000),
                        rampDuration = 5.minutes
                    )
                )
                weightedWorkload(
                    weight = 10
                )
            }
        }

        val output = json.encodeToString(benchmark)
        println(output)

        val decoded = json.decodeFromString<Benchmark>(output)

        decoded.stages.filterIsInstance<ParallelStage>().first().timeout shouldBe 5.milliseconds
    }

    test("template is deserialized correctly") {
        val benchmark = buildBenchmark {
            sequentialStage {
                rateWorkload(
                    count = Long.MAX_VALUE
                )
            }
        }

        val decodedBenchmark = Json.decodeFromString<Benchmark>(json.encodeToString(benchmark))

        decodedBenchmark.stages
            .shouldHaveSize(1)
            .first()
            .workloads
            .shouldHaveSize(1)
            .first()
            .executor
            .shouldBeInstanceOf<InsertOneExecutor>()
            .template["name"]
            .shouldBeInstanceOf<Operator<String>>()
    }

    test("decoding invalid rate throws exception") {
        val str = readResource("/benchmarkInvalid.json")

        shouldThrow<IllegalArgumentException> {
            json.decodeFromString<Benchmark>(str)
        }
    }

})