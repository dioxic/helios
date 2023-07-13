package uk.dioxic.helios.generate

import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldContainKeys
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.bson.buildBsonDocument
import org.bson.BsonDocument
import org.bson.BsonInvalidOperationException
import org.bson.Document
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.json.JsonMode
import org.bson.json.JsonWriter
import org.bson.json.JsonWriterSettings
import org.bson.types.ObjectId
import uk.dioxic.helios.generate.codecs.DocumentCodec
import uk.dioxic.helios.generate.operators.*
import uk.dioxic.helios.generate.test.shouldBeWrapped
import java.io.StringWriter

class DocumentCodecTests : FunSpec({

    fun BsonDocument.decode(): Document {
        println("bson: $this")
        return DocumentCodec().decode(asBsonReader(), DecoderContext.builder().build())
    }

    context("decode") {

//        fun decodeAndPrint(document: BsonDocument): Document {
//            println("bson: $document")
//            return DocumentCodec().decode(document.asBsonReader(), DecoderContext.builder().build())
//        }

        test("top level operators") {
            val definition = buildBsonDocument {
                putOperator<NameOperator>("name")
                putOperator<ObjectIdOperator>("oid")
            }

            definition.decode().should { document ->
                document.shouldContainKeys("name", "oid")
                document["name"].shouldBeInstanceOf<NameOperator>()
                document["oid"].shouldBeInstanceOf<ObjectIdOperator>()
            }

        }

        test("nested operators") {
            val definition = buildBsonDocument {
                putBsonDocument("subDoc") {
                    putBsonDocument("subSubDoc") {
                        putOperator<ObjectIdOperator>("oid")
                    }
                }
            }

            definition.decode().should { document ->
                "verifying document=$document".asClue {
                    document shouldContainKey "subDoc"
                    document["subDoc"].should { subDoc ->
                        "verifying subDoc=$subDoc".asClue {
                            subDoc.shouldBeInstanceOf<Map<String, Any>>()
                            subDoc shouldContainKey "subSubDoc"
                            subDoc["subSubDoc"].should { subSubDoc ->
                                "verifying subSubDoc=$subSubDoc".asClue {
                                    subSubDoc.shouldBeInstanceOf<Map<String, Any>>()
                                    subSubDoc shouldContainKey "oid"
                                    subSubDoc["oid"].shouldBeInstanceOf<ObjectIdOperator>()
                                }
                            }
                        }
                    }
                }
            }

        }

        test("operators as input to operators") {
            val definition = buildBsonDocument {
                putOperatorObject<ArrayOperator>("array") {
                    putOperator<ObjectIdOperator>("of")
                    put("number", 3)
                }
            }

            definition.decode().should { document ->
                document shouldContainKey "array"
                document["array"]
                    .shouldBeInstanceOf<ArrayOperator>()
                    .should { array ->
                        array.of.shouldBeInstanceOf<ObjectIdOperator>()
                        array.number.shouldBeWrapped<Int>() shouldBe 3
                    }

            }
        }

        test("map with nested operators as input to operators") {
            val definition = buildBsonDocument {
                putOperatorObject<ArrayOperator>("array") {
                    putBsonDocument("of") {
                        putOperator<NameOperator>("name")
                        putOperator<ObjectIdOperator>("id")
                    }
                    put("number", 3)
                }
            }

            definition.decode().should { document ->
                document shouldContainKey "array"
                document["array"]
                    .shouldBeInstanceOf<ArrayOperator>()
                    .should { array ->
                        array.of.shouldBeWrapped<Map<String, Any?>> { of ->
                            of.shouldHaveSize(2)
                            of.shouldContainKeys("name", "id")
                            of["name"].shouldBeInstanceOf<NameOperator>()
                            of["id"].shouldBeInstanceOf<ObjectIdOperator>()
                        }
                        array.number.shouldBeWrapped<Int>() shouldBe 3
                    }
            }

        }

        test("array with nested operators as input to operators") {
            val definition = buildBsonDocument {
                putOperatorObject<ArrayOperator>("array") {
                    putBsonArray("of") {
                        addOperator<ObjectIdOperator>()
                        addOperator<ObjectIdOperator>()
                    }
                    put("number", 3)
                }
            }

            definition.decode().should { document ->
                document shouldContainKey "array"
                document["array"]
                    .shouldBeInstanceOf<ArrayOperator>()
                    .should { array ->
                        array.of.shouldBeWrapped<List<*>> { of ->
                            of.shouldHaveSize(2)
                            of.shouldForAll { it.shouldBeInstanceOf<ObjectIdOperator>() }
                        }
                        array.number.shouldBeWrapped<Int>() shouldBe 3
                    }
            }


        }
    }

    context("encoding") {
        val rootKey = getOperatorKey<RootOperator>()
        val writerSettings = JsonWriterSettings.builder()
            .indent(true)
            .outputMode(JsonMode.RELAXED)
            .build()

        fun encode(document: Document, collectible: Boolean = true): Document {
            val writer = JsonWriter(StringWriter(), writerSettings)
            val encoderContext = EncoderContext.builder().isEncodingCollectibleDocument(collectible).build()
            DocumentCodec().encode(writer, document, encoderContext)
            return writer.writer.toString().let { json ->
                Document.parse(json)
            }
        }

        test("nested operators") {
            val cities = listOf("London", "Belfast")
            val document = mapOf(
                "address" to mapOf(
                    "cities" to ChooseOperator(from = { cities })
                )
            ).toDocument()

            encode(document).should {
                it.shouldContainKeys("address")
                it["address"].shouldBeInstanceOf<Document>().should { address ->
                    address shouldContainKey "cities"
                    address["cities"] shouldBeIn cities
                }
            }
        }

        context("root operator") {

            test("should fail when root operator is not a document") {
                val document = mapOf(rootKey to RootOperator { "halibut" }).toDocument()

                shouldThrowExactly<BsonInvalidOperationException> {
                    encode(document)
                }
            }

            test("operator without _id") {
                val personDoc = mapOf(
                    "type" to "person",
                    "height" to 12
                ).toDocument()
                val orgDoc = mapOf(
                    "type" to "org",
                    "orgId" to 123
                ).toDocument()
                val document = mapOf(rootKey to RootOperator(
                    ChooseOperator(
                        from = { listOf(personDoc, orgDoc) }
                    )
                )).toDocument()

                encode(document).should {
                    it.shouldContainKeys("_id", "type")
                    it["_id"].shouldBeInstanceOf<ObjectId>()
                    it["type"] shouldBeIn listOf("person", "org")
                }
            }

            test("operator with _id") {
                val personDoc = mapOf(
                    "_id" to "Bob",
                    "type" to "person",
                    "height" to 12
                ).toDocument()
                val document = mapOf(rootKey to RootOperator(
                    ChooseOperator(
                        from = { listOf(personDoc) }
                    )
                )).toDocument()

                encode(document).should {
                    it.shouldContainKeys("_id", "type")
                    it["_id"] shouldBe "Bob"
                    it["type"] shouldBeIn listOf("person", "org")
                }
            }

            test("document with _id") {
                val root = mapOf(
                    "_id" to "myId",
                    "name" to "Bob",
                ).toDocument()
                val document = mapOf(rootKey to RootOperator { root }).toDocument()

                encode(document, true).should {
                    it.shouldContainKeys("_id", "name")
                    it["_id"] shouldBe root["_id"]
                    it["name"] shouldBe root["name"]
                }
            }

            test("document without _id") {
                val root = mapOf(
                    "name" to "Bob",
                ).toDocument()
                val document = mapOf(rootKey to RootOperator { root }).toDocument()

                encode(document, true).should {
                    it.shouldContainKeys("_id", "name")
                    it["_id"].shouldBeInstanceOf<ObjectId>()
                    it["name"] shouldBe root["name"]
                }
            }
        }
    }
})