package uk.dioxic.helios.execute

import com.mongodb.ReadConcern
import com.mongodb.ReadPreference
import com.mongodb.TransactionOptions
import com.mongodb.WriteConcern
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.resource.resourceAsString
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.bson.Bson
import kotlinx.serialization.bson.BsonValueSerializer
import kotlinx.serialization.bson.buildBsonDocument
import kotlinx.serialization.bson.toBson
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.bson.BsonDocument
import org.bson.BsonInt32
import org.bson.BsonString
import org.bson.BsonValue
import uk.dioxic.helios.execute.model.*
import uk.dioxic.helios.execute.serialization.TransactionOptionsSerializer
import uk.dioxic.helios.execute.serialization.UpdateOptionsSerializer
import uk.dioxic.helios.execute.serialization.WriteConcernSerializer
import uk.dioxic.helios.generate.Operator
import uk.dioxic.helios.generate.buildTemplate
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SerializationTests : FunSpec({

    fun stringifyAndPrint(bsonValue: BsonValue) =
        Bson.encodeToString(BsonValueSerializer, bsonValue).also {
            println(it)
        }

    context("WriteConcern") {
        context("serialization") {

            test("majority") {
                Bson.encodeToBsonDocument(WriteConcernSerializer, WriteConcern.MAJORITY).should {
                    it.shouldBeInstanceOf<BsonDocument>()
                    it["w"].should { w ->
                        w.shouldNotBeNull()
                        w.shouldBeInstanceOf<BsonString>()
                    }
                }
            }
            test("w1") {
                Bson.encodeToBsonDocument(WriteConcernSerializer, WriteConcern.W1).should {
                    it.shouldBeInstanceOf<BsonDocument>()
                    it["w"].should { w ->
                        w.shouldNotBeNull()
                        w.shouldBeInstanceOf<BsonInt32>()
                    }
                }
            }
        }
        context("deserialization") {
            test("{ w: 'majority' }") {
                val str = stringifyAndPrint(
                    buildBsonDocument {
                        put("w", "majority")
                    }
                )

                Bson.decodeFromString(WriteConcernSerializer, str).should {
                    it.wObject shouldBe "majority"
                }
            }

            test("{ w: 'majority', wtimeout: 1000 }") {
                val str = stringifyAndPrint(
                    buildBsonDocument {
                        put("w", "majority")
                        put("wtimeout", 1000)
                    }
                )

                Bson.decodeFromString(WriteConcernSerializer, str).should {
                    it.wObject shouldBe "majority"
                    it.getWTimeout(TimeUnit.MILLISECONDS) shouldBe 1000
                }
            }

            test("{ w: 1 }") {
                val str = stringifyAndPrint(
                    buildBsonDocument {
                        put("w", 1)
                    }
                )

                Bson.decodeFromString(WriteConcernSerializer, str).should {
                    it.wObject shouldBe 1
                }
            }

            test("{ w: 1, j: false }") {
                val str = stringifyAndPrint(
                    buildBsonDocument {
                        put("w", 1)
                        put("j", false)
                    }
                )

                Bson.decodeFromString(WriteConcernSerializer, str).should {
                    it.wObject shouldBe 1
                    it.journal shouldBe false
                }
            }
        }
    }

    context("ReadConcern") {
        val dc = DataClassWithReadConcern(ReadConcern.SNAPSHOT)
        val json = """
            { "readConcern": "snapshot" }
        """.trimIndent()

        test("deserialize") {
            Bson.decodeFromString<DataClassWithReadConcern>(json).should {
                it.readConcern shouldBe ReadConcern.SNAPSHOT
            }
        }

        test("serialize") {
            Bson.encodeToString(dc) shouldBeJson json
        }
    }

    context("ReadPreference") {
        val dc = DataClassWithReadPreference(ReadPreference.nearest())
        val json = """
            { "readPreference": "nearest" }
        """.trimIndent()

        test("deserialize") {
            Bson.decodeFromString<DataClassWithReadPreference>(json).should {
                it.readPreference.name shouldBe ReadPreference.nearest().name
            }
        }

        test("serialize") {
            Bson.encodeToString(dc).should {
                it shouldBeJson json
            }
        }
    }

    context("Store") {
        context("Path Store") {
            val dc = DataClassWithStore(Store.YES)
            val json = """
                { "store": true }
            """.trimIndent()
            test("serialize") {
                Bson.encodeToString(dc) shouldBeJson json
            }
            test("deserialize"){
                Bson.decodeFromString<DataClassWithStore>(json) shouldBe dc
            }
        }
        context("Boolean Store") {
            val dc = DataClassWithStore(PathStore("/myPath/file.json"))
            val json = """
                { "store": "/myPath/file.json" }
            """.trimIndent()
            test("serialize") {
                Bson.encodeToString(dc) shouldBeJson json
            }
            test("deserialize"){
                Bson.decodeFromString<DataClassWithStore>(json) shouldBe dc
            }
        }
    }

    context("UpdateOptions") {
        val optionsStr = stringifyAndPrint(buildBsonDocument {
            put("upsert", true)
            put("bypassDocumentValidation", true)
            put("hintString", "myHint")
            putBsonDocument("hint") {
                put("status", 1)
            }
            put("comment", "myComment")
            putBsonArray("arrayFilters") {
                addBsonDocument {
                    put("first.a", 123)
                }
                addBsonDocument {
                    put("second.b", "abc")
                }
            }
        })

        test("deserialize") {
            Bson.decodeFromString(UpdateOptionsSerializer, optionsStr).should {
                it.isUpsert shouldBe true
                it.comment?.asString()?.value shouldBe "myComment"
                it.hintString shouldBe "myHint"
                it.hint.shouldBeInstanceOf<BsonDocument>().should {hint ->
                    hint["status"] shouldBe 1.toBson()
                }
                it.bypassDocumentValidation shouldBe true
            }
        }
    }

    context("TransactionOptions") {
        val txOptionsStr = stringifyAndPrint(buildBsonDocument {
            put("readConcern", "available")
            put("readPreference", "nearest")
            put("writeConcern", 1)
            put("maxCommitTimeMS", 1000)
        })

        test("deserialize") {
            Bson.decodeFromString(TransactionOptionsSerializer, txOptionsStr).should {
                it.readConcern shouldBe ReadConcern.AVAILABLE
                it.writeConcern shouldBe WriteConcern.W1
                it.readPreference.shouldNotBeNull().name shouldBe ReadPreference.nearest().name
                it.getMaxCommitTime(TimeUnit.MILLISECONDS) shouldBe 1000
            }
        }

        test("serialize") {
            val txOptions = TransactionOptions.builder()
                .readConcern(ReadConcern.AVAILABLE)
                .writeConcern(WriteConcern.W1)
                .maxCommitTime(1000, TimeUnit.MILLISECONDS)
                .readPreference(ReadPreference.nearest())
                .build()

            Bson.encodeToBsonDocument(TransactionOptionsSerializer, txOptions).should {
                it.shouldBeInstanceOf<BsonDocument>()
                it["readConcern"].should { rc ->
                    rc shouldBe "available".toBson()
                }
                it["readPreference"].should { rp ->
                    rp shouldBe "nearest".toBson()
                }
                it["writeConcern"].should { wc ->
                    wc.shouldBeInstanceOf<BsonDocument>()
                }
                it["maxCommitTimeMS"].should { wc ->
                    wc shouldBe 1000L.toBson()
                }
            }
        }
    }

    val bson = Bson {
        prettyPrint = true
    }

    context("Benchmark") {

        test("can handle 64-bit integer") {
            val benchmark = buildBenchmark {
                sequentialStage {
                    rateWorkload(
                        executor = defaultExecutor,
                        count = Long.MAX_VALUE
                    )
                }
            }

            val str = bson.encodeToString(benchmark)
            println(str)
            val decoded = bson.decodeFromString<Benchmark>(str)
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
            val str = bson.encodeToString(benchmark)
            println(str)
            val decoded = bson.decodeFromString<Benchmark>(str)
            decoded.stages[0].workloads.first().executor.shouldBeInstanceOf<CommandExecutor>()
        }

        test("insertOne executor") {
            val benchmark = buildBenchmark {
                sequentialStage {
                    rateWorkload(
                        executor = InsertOneExecutor(
                            database = "test",
                            collection = "people",
                            template = buildTemplate { put("name", "\$name") }
                        )
                    )
                }
            }
            val str = bson.encodeToString(benchmark)
            println(str)
            val decoded = bson.decodeFromString<Benchmark>(str)
            decoded.stages[0].workloads.first().executor.shouldBeInstanceOf<InsertOneExecutor>()
        }

        test("polymorphic deserialization works without type") {
            val str = """
        {
            "type": "parallel",
            "name": "stage0",
            "workloads": [
                {
                    "name": "workload0",
                    "executor": {
                        "type": "insertOne",
                        "database": "myDB",
                        "collection": "myCollection",
                        "template": {
                            "name": "${'$'}name"
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

            val stage = bson.decodeFromString<Stage>(str)

            stage.shouldBeInstanceOf<ParallelStage>()
                .workloads.should {
                    it.shouldHaveSize(2)
                    it[0].shouldBeInstanceOf<WeightedWorkload>()
                    it[1].shouldBeInstanceOf<RateWorkload>()
                        .rate.shouldBeInstanceOf<TpsRate>()
                }

            println(bson.encodeToString(stage))

        }

        test("basic") {
            val benchmark = buildBenchmark {
                sequentialStage {
                    rateWorkload(
                        executor = defaultExecutor,
                    )
                }
                parallelStage(timeout = 5.milliseconds) {
                    rateWorkload(
                        executor = defaultExecutor,
                        rate = TpsRate(tps = 500),
                    )
                    rateWorkload(
                        executor = defaultExecutor,
                        rate = UnlimitedRate
                    )
                    rateWorkload(
                        executor = defaultExecutor,
                        rate = RampedRate(
                            from = PeriodRate(period = 1.seconds),
                            to = TpsRate(tps = 9000),
                            rampDuration = 5.minutes
                        )
                    )
                    weightedWorkload(
                        executor = defaultExecutor,
                        weight = 10
                    )
                }
            }

            val output = bson.encodeToString(benchmark)
            println(output)

            val decoded = bson.decodeFromString<Benchmark>(output)

            decoded.stages.filterIsInstance<ParallelStage>().first().timeout shouldBe 5.milliseconds
        }

        test("template is deserialized correctly") {
            val benchmark = buildBenchmark {
                sequentialStage {
                    rateWorkload(
                        executor = defaultMongoExecutor,
                        count = Long.MAX_VALUE
                    )
                }
            }

            val decodedBenchmark = Bson.decodeFromString<Benchmark>(bson.encodeToString(benchmark))

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
            val str = resourceAsString("/benchmarkInvalid.json")

            shouldThrow<IllegalArgumentException> {
                println(bson.decodeFromString<Benchmark>(str))
            }.also {
                println(it)
            }
        }

    }

})