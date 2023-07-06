package uk.dioxic.helios.execute.serialization

import com.mongodb.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.bson.Bson
import kotlinx.serialization.bson.BsonValueSerializer
import kotlinx.serialization.bson.buildBsonDocument
import kotlinx.serialization.bson.toBson
import org.bson.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalSerializationApi::class)
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
        test("deserialize") {
            Bson.decodeFromString(ReadConcernSerializer, "\"majority\"").should {
                it.level shouldBe ReadConcernLevel.MAJORITY
            }
        }

        test("serialize") {
            Bson.encodeToString(ReadConcernSerializer, ReadConcern.MAJORITY).should {
                it shouldBe "\"majority\""
            }
        }
    }

    context("ReadPreference") {
        test("deserialize") {
            Bson.decodeFromString(ReadPreferenceSerializer, "\"secondaryPreferred\"").should {
                it.name shouldBe ReadPreference.secondaryPreferred().name
            }
        }

        test("serialize") {
            Bson.encodeToString(ReadPreferenceSerializer, ReadPreference.secondaryPreferred()).should {
                it shouldBe "\"secondaryPreferred\""
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
                it.hint.shouldBeInstanceOf<Document>().should {hint ->
                    hint["status"] shouldBe 1
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


})