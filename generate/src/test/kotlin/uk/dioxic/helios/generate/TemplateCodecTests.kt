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
import kotlinx.serialization.bson.Bson
import kotlinx.serialization.bson.buildBsonDocument
import kotlinx.serialization.bson.decodeFromBsonDocument
import org.bson.BsonDocument
import org.bson.Document
import org.bson.codecs.EncoderContext
import org.bson.json.JsonMode
import org.bson.json.JsonWriter
import org.bson.json.JsonWriterSettings
import org.bson.types.ObjectId
import uk.dioxic.helios.generate.operators.*
import uk.dioxic.helios.generate.test.shouldBeWrapped
import java.io.StringWriter

class TemplateCodecTests : FunSpec({

    context("decode") {

        fun decodeAndPrint(document: BsonDocument): Template {
            println("bson: $document")
            return Bson.decodeFromBsonDocument<Template>(document).also {
                println("template: $it")
            }
        }

        test("top level operators") {
            val document = buildBsonDocument {
                putOperator<NameOperator>("name")
                putOperator<ObjectIdOperator>("oid")
            }

            decodeAndPrint(document).should { template ->
                template.shouldContainKeys("name", "oid")
                template["name"].shouldBeInstanceOf<NameOperator>()
                template["oid"].shouldBeInstanceOf<ObjectIdOperator>()
            }

        }

        test("nested operators") {
            val document = buildBsonDocument {
                putBsonDocument("subDoc") {
                    putBsonDocument("subSubDoc") {
                        putOperator<ObjectIdOperator>("oid")
                    }
                }
            }

            decodeAndPrint(document).should { template ->
                "verifying template=$template".asClue {
                    template shouldContainKey "subDoc"
                    template["subDoc"].should { subDoc ->
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
            val document = buildBsonDocument {
                putOperatorObject<ArrayOperator>("array") {
                    putOperator<ObjectIdOperator>("of")
                    put("number", 3)
                }
            }

            decodeAndPrint(document).should { template ->
                template shouldContainKey "array"
                template["array"]
                    .shouldBeInstanceOf<ArrayOperator>()
                    .should { array ->
                        array.of.shouldBeInstanceOf<ObjectIdOperator>()
                        array.number.shouldBeWrapped<Int>() shouldBe 3
                    }

            }
        }

        test("map with nested operators as input to operators") {
            val document = buildBsonDocument {
                putOperatorObject<ArrayOperator>("array") {
                    putBsonDocument("of") {
                        putOperator<NameOperator>("name")
                        putOperator<ObjectIdOperator>("id")
                    }
                    put("number", 3)
                }
            }

            decodeAndPrint(document).should { template ->
                template shouldContainKey "array"
                template["array"]
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
            val document = buildBsonDocument {
                putOperatorObject<ArrayOperator>("array") {
                    putBsonArray("of") {
                        addOperator<ObjectIdOperator>()
                        addOperator<ObjectIdOperator>()
                    }
                    put("number", 3)
                }
            }

            decodeAndPrint(document).should { template ->
                template shouldContainKey "array"
                template["array"]
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

        fun encodeAndPrint(template: Template, collectible: Boolean = false): Document {
            val writer = JsonWriter(StringWriter(), writerSettings)
            val encoderContext = EncoderContext.builder().isEncodingCollectibleDocument(collectible).build()
            Template.defaultCodec.encode(writer, template, encoderContext)
            val json = writer.writer.toString()
            println(json)
            return Document.parse(json)
        }

        test("root operator is a map containing operators") {
            val cities = listOf("London", "Belfast")
            val root = mapOf(
                "address" to mapOf(
                    "cities" to ChooseOperator(from = { cities })
                )
            )
            val template = templateOf(rootKey to root)

            encodeAndPrint(template).should {
                it shouldContainKey "address"
                it["address"].shouldBeInstanceOf<Document>().should { address ->
                    address shouldContainKey "cities"
                    println(address["cities"])
                    address["cities"] shouldBeIn cities
                }
            }
        }

        test("root operator is a simple map") {
            val template = templateOf(
                rootKey to mapOf("animal" to "halibut")
            )

            encodeAndPrint(template).should {
                it shouldContainKey "animal"
                it["animal"] shouldBe "halibut"
            }
        }

        test("should fail when root operator is not a map") {
            val template = templateOf(rootKey to "halibut")

            shouldThrowExactly<IllegalArgumentException> {
                encodeAndPrint(template)
            }
        }

        test("root operator is an operator") {
            val personMap = mapOf(
                "type" to "person",
                "height" to 12
            )
            val orgMap = mapOf(
                "type" to "org",
                "orgId" to 123
            )
            val template = templateOf(rootKey to ChooseOperator(
                from = { listOf(personMap, orgMap) }
            ))

            encodeAndPrint(template).should {
                it shouldContainKey "type"
                it["type"] shouldBeIn listOf("person", "org")
            }
        }

        test("takes the _id from the root operator if present") {
            val root = mapOf(
                "_id" to "myId",
                "name" to "Bob",
            )
            val template = templateOf(rootKey to root)

            Template.defaultCodec.generateIdIfAbsentFromDocument(template)

            encodeAndPrint(template, true).should {
                it.shouldContainKeys("_id", "name")
                it["_id"] shouldBe root["_id"]
                it["name"] shouldBe root["name"]
            }
        }

        test("takes the _id from the template if not present on the root") {
            val root = mapOf(
                "name" to "Bob",
            )
            val template = templateOf(rootKey to root)

            Template.defaultCodec.generateIdIfAbsentFromDocument(template)

            encodeAndPrint(template, true).should {
                it.shouldContainKeys("_id", "name")
                it["_id"].shouldBeInstanceOf<ObjectId>()
                it["name"] shouldBe "Bob"
            }
        }
    }

})