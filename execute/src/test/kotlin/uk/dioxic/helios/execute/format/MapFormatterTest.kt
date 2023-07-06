package uk.dioxic.helios.execute.format

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContainKeys
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.should
import kotlinx.serialization.bson.buildBsonDocument
import org.bson.BsonArray
import org.bson.BsonDocument
import org.bson.BsonElement
import org.bson.BsonString
import uk.dioxic.helios.generate.flatten
import java.time.LocalDateTime

class MapFormatterTest : FunSpec({

    context("flatten map") {
        context("with branches") {
            test("non-nested map is correct") {
                val map = mapOf(
                    "name" to "Bob",
                    "date" to LocalDateTime.now(),
                    "n" to 33
                )

                map.flatten().should {
                    println(it)
                    it shouldHaveSize 3
                    it.shouldContainKeys("name", "date", "n")
                }
            }

            test("nested map is correct") {
                val map = mapOf(
                    "name" to "Bob",
                    "nested" to mapOf(
                        "date" to LocalDateTime.now(),
                        "n" to 33
                    )
                )

                map.flatten().should {
                    println(it)
                    it shouldHaveSize 4
                    it.shouldContainKeys("name", "nested.date", "nested.n", "nested")
                }
            }

            test("nested array is correct") {
                val map = mapOf(
                    "name" to "Bob",
                    "nested" to listOf("date", "n")
                )

                map.flatten().should {
                    println(it)
                    it shouldHaveSize 4
                    it.shouldContainKeys("name", "nested.0", "nested.1", "nested")
                }
            }

            test("complex nested object is correct") {
                val map = mapOf(
                    "name" to "Bob",
                    "nested" to mapOf(
                        "array" to listOf(mapOf("date" to "myDate"), "n")
                    )
                )

                map.flatten().should {
                    println(it)
                    it shouldHaveSize 6
                    it.shouldContainKeys(
                        "name",
                        "nested",
                        "nested.array",
                        "nested.array.0",
                        "nested.array.0.date",
                        "nested.array.1"
                    )
                }
            }
        }
        context("leaves only") {
            test("non-nested map is correct") {
                val map = mapOf(
                    "name" to "Bob",
                    "date" to LocalDateTime.now(),
                    "n" to 33
                )

                map.flatten(leafOnly = true).should {
                    println(it)
                    it shouldHaveSize 3
                    it.shouldContainKeys("name", "date", "n")
                }
            }

            test("nested map is correct") {
                val map = mapOf(
                    "name" to "Bob",
                    "nested" to mapOf(
                        "date" to LocalDateTime.now(),
                        "n" to 33
                    )
                )

                map.flatten(leafOnly = true).should {
                    println(it)
                    it shouldHaveSize 3
                    it.shouldContainKeys("name", "nested.date", "nested.n")
                }
            }

            test("nested array is correct") {
                val map = mapOf(
                    "name" to "Bob",
                    "nested" to listOf("date", "n")
                )

                map.flatten(leafOnly = true).should {
                    println(it)
                    it shouldHaveSize 3
                    it.shouldContainKeys("name", "nested.0", "nested.1")
                }
            }

            test("complex nested object is correct") {
                val map = mapOf(
                    "name" to "Bob",
                    "nested" to mapOf(
                        "array" to listOf(mapOf("date" to "myDate"), "n")
                    )
                )

                map.flatten(leafOnly = true).should {
                    println(it)
                    it shouldHaveSize 3
                    it.shouldContainKeys("name", "nested.array.0.date", "nested.array.1")
                }
            }
        }
    }

    context("JsonObject") {
        test("complex nested object is correct") {
            val jsonObject = buildBsonDocument {
                put("name", "Bob")
                putBsonDocument("nested") {
                    putBsonArray("array") {
                        addBsonDocument {
                            put("date", "myDate")
                        }
                        add("n")
                    }
                }
            }

            jsonObject.flatten().should {
                println(it)
                it shouldHaveSize 6
                it.shouldContainKeys(
                    "name",
                    "nested",
                    "nested.array",
                    "nested.array.0",
                    "nested.array.0.date",
                    "nested.array.1"
                )
            }
        }
    }

    context("BsonDocument") {
        test("complex nested object is correct") {
            val bsonDocument = BsonDocument(
                listOf(
                    BsonElement("name", BsonString("Bob")),
                    BsonElement(
                        "nested", BsonDocument(
                            listOf(
                                BsonElement(
                                    "array", BsonArray(
                                        listOf(
                                            BsonDocument(listOf(BsonElement("date", BsonString("myDate")))),
                                            BsonString("n")
                                        )
                                    )
                                ),
                            )
                        )
                    )
                )
            )

            bsonDocument.flatten().should {
                println(it)
                it shouldHaveSize 6
                it.shouldContainKeys(
                    "name",
                    "nested",
                    "nested.array",
                    "nested.array.0",
                    "nested.array.0.date",
                    "nested.array.1"
                )
            }
        }
    }
})