package uk.dioxic.helios.execute.serialization

import com.mongodb.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.bson.Document
import java.util.concurrent.TimeUnit

class SerializationTests : FunSpec({

    fun stringifyAndPrint(jsonElement: JsonElement) =
        Json.encodeToString(jsonElement).also {
            println(it)
        }

    context("WriteConcern") {
        context("serialization") {

            test("majority") {
                Json.encodeToJsonElement(WriteConcernSerializer, WriteConcern.MAJORITY).should {
                    it.shouldBeInstanceOf<JsonObject>()
                    it["w"].should { w ->
                        w.shouldNotBeNull()
                        w.shouldBeInstanceOf<JsonPrimitive>()
                        w.isString shouldBe true
                    }
                }
            }
            test("w1") {
                Json.encodeToJsonElement(WriteConcernSerializer, WriteConcern.W1).should {
                    it.shouldBeInstanceOf<JsonObject>()
                    it["w"].should { w ->
                        w.shouldNotBeNull()
                        w.shouldBeInstanceOf<JsonPrimitive>()
                        w.isString shouldBe false
                        w.intOrNull.shouldNotBeNull()
                    }
                }
            }
        }
        context("deserialization") {
            test("{ w: 'majority' }") {
                val str = stringifyAndPrint(
                    buildJsonObject {
                        put("w", "majority")
                    }
                )

                Json.decodeFromString(WriteConcernSerializer, str).should {
                    it.wObject shouldBe "majority"
                }
            }

            test("{ w: 'majority', wtimeout: 1000 }") {
                val str = stringifyAndPrint(
                    buildJsonObject {
                        put("w", "majority")
                        put("wtimeout", 1000)
                    }
                )

                Json.decodeFromString(WriteConcernSerializer, str).should {
                    it.wObject shouldBe "majority"
                    it.getWTimeout(TimeUnit.MILLISECONDS) shouldBe 1000
                }
            }

            test("{ w: 1 }") {
                val str = stringifyAndPrint(
                    buildJsonObject {
                        put("w", 1)
                    }
                )

                Json.decodeFromString(WriteConcernSerializer, str).should {
                    it.wObject shouldBe 1
                }
            }

            test("{ w: 1, j: false }") {
                val str = stringifyAndPrint(
                    buildJsonObject {
                        put("w", 1)
                        put("j", false)
                    }
                )

                Json.decodeFromString(WriteConcernSerializer, str).should {
                    it.wObject shouldBe 1
                    it.journal shouldBe false
                }
            }
        }
    }

    context("ReadConcern") {
        test("deserialize") {
            Json.decodeFromString(ReadConcernSerializer, "\"majority\"").should {
                it.level shouldBe ReadConcernLevel.MAJORITY
            }
        }

        test("serialize") {
            Json.encodeToString(ReadConcernSerializer, ReadConcern.MAJORITY).should {
                it shouldBe "\"majority\""
            }
        }
    }

    context("ReadPreference") {
        test("deserialize") {
            Json.decodeFromString(ReadPreferenceSerializer, "\"secondaryPreferred\"").should {
                it.name shouldBe ReadPreference.secondaryPreferred().name
            }
        }

        test("serialize") {
            Json.encodeToString(ReadPreferenceSerializer, ReadPreference.secondaryPreferred()).should {
                it shouldBe "\"secondaryPreferred\""
            }
        }
    }

    context("UpdateOptions") {
        val optionsStr = stringifyAndPrint(buildJsonObject {
            put("upsert", true)
            put("bypassDocumentValidation", true)
            put("hintString", "myHint")
            putJsonObject("hint") {
                put("status", 1)
            }
            put("comment", "myComment")
            putJsonArray("arrayFilters") {
                addJsonObject {
                    put("first.a", 123)
                }
                addJsonObject {
                    put("second.b", "abc")
                }
            }
        })

        test("deserialize") {
            Json.decodeFromString(UpdateOptionsSerializer, optionsStr).should {
                it.isUpsert shouldBe true
                it.comment?.asString()?.value shouldBe "myComment"
                it.hintString shouldBe "myHint"
                it.hint.shouldBeInstanceOf<Document>().should {hint ->
                    hint["status"] shouldBe 1
                }
                it.bypassDocumentValidation shouldBe true
            }
        }
    }

    context("TransactionOptions") {
        val txOptionsStr = stringifyAndPrint(buildJsonObject {
            put("readConcern", "available")
            put("readPreference", "nearest")
            put("writeConcern", 1)
            put("maxCommitTimeMS", 1000)
        })

        test("deserialize") {

            Json.decodeFromString(TransactionOptionsSerializer, txOptionsStr).should {
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

            Json.encodeToJsonElement(TransactionOptionsSerializer, txOptions).should {
                it.shouldBeInstanceOf<JsonObject>()
                it["readConcern"].should { rc ->
                    rc.shouldBeInstanceOf<JsonPrimitive>().content shouldBe "available"
                }
                it["readPreference"].should { rp ->
                    rp.shouldBeInstanceOf<JsonPrimitive>().content shouldBe "nearest"
                }
                it["writeConcern"].should { wc ->
                    wc.shouldBeInstanceOf<JsonObject>()
                }
                it["maxCommitTimeMS"].should { wc ->
                    wc.shouldBeInstanceOf<JsonPrimitive>().intOrNull shouldBe 1000
                }
            }
        }
    }


})