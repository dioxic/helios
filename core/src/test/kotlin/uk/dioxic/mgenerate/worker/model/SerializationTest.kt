package uk.dioxic.mgenerate.worker.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import uk.dioxic.mgenerate.operators.Operator
import uk.dioxic.mgenerate.test.benchmark
import uk.dioxic.mgenerate.test.readResource
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SerializationTest : FunSpec({

    val json = Json {
        prettyPrint = true
    }

    test("can handle 64-bit integer") {
        val benchmark = benchmark {
            sequentialStage {
                workload(
                    count = Long.MAX_VALUE
                )
            }
        }

        val str = json.encodeToString(benchmark)
        println(str)
        val decoded = json.decodeFromString<Benchmark>(str)
        decoded.stages[0].workloads.first().count shouldBeExactly Long.MAX_VALUE
    }

    test("basic") {
        val benchmark = benchmark {
            sequentialStage {
                workload()
            }
            parallelStage(timeout = 5.milliseconds) {
                workload(
                    weight = 2.0,
                    rate = FixedRate(tps = 500),
                )
                workload(
                    weight = 2.0,
                    rate = RampedRate(
                        delay = 5.seconds,
                        from = FixedRate(every = 1.seconds),
                        to = FixedRate(tps = 9000),
                        rampDuration = 5.minutes
                    )
                )
            }
        }

        val output = json.encodeToString(benchmark)
        println(output)

        val decoded = json.decodeFromString<Benchmark>(output)

        decoded.stages.filterIsInstance<ParallelStage>().first().timeout shouldBe 5.milliseconds
    }

    test("template is deserialized correctly") {
        val benchmark = benchmark {
            sequentialStage {
                workload(
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