package uk.dioxic.mgenerate.execute.format

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.should
import kotlinx.serialization.json.*
import org.bson.BsonArray
import org.bson.BsonDocument
import org.bson.BsonElement
import org.bson.BsonString
import java.time.LocalDateTime

class MapFormatterTest : FunSpec({

    context("map") {

        test("non-nested map is correct") {
            val map = mapOf(
                "name" to "Bob",
                "date" to LocalDateTime.now(),
                "n" to 33
            )

            map.flatten('.').should {
                println(it)
                it shouldHaveSize 3
                it shouldContainKey "name"
                it shouldContainKey "date"
                it shouldContainKey "n"
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

            map.flatten('.').should {
                println(it)
                it shouldHaveSize 3
                it shouldContainKey "name"
                it shouldContainKey "nested.date"
                it shouldContainKey "nested.n"
            }
        }

        test("nested array is correct") {
            val map = mapOf(
                "name" to "Bob",
                "nested" to listOf("date", "n")
            )

            map.flatten('.').should {
                println(it)
                it shouldHaveSize 3
                it shouldContainKey "name"
                it shouldContainKey "nested.0"
                it shouldContainKey "nested.1"
            }
        }

        test("complex nested object is correct") {
            val map = mapOf(
                "name" to "Bob",
                "nested" to mapOf(
                    "array" to listOf(mapOf("date" to "myDate"), "n")
                )
            )

            map.flatten('.').should {
                println(it)
                it shouldHaveSize 3
                it shouldContainKey "name"
                it shouldContainKey "nested.array.0.date"
                it shouldContainKey "nested.array.1"
            }
        }
    }

    context("JsonObject") {
        test("complex nested object is correct") {
            val jsonObject = buildJsonObject {
                put("name", "Bob")
                putJsonObject("nested") {
                    putJsonArray("array") {
                        addJsonObject {
                            put("date", "myDate")
                        }
                        add("n")
                    }
                }
            }

            jsonObject.flatten('.').should {
                println(it)
                it shouldHaveSize 3
                it shouldContainKey "name"
                it shouldContainKey "nested.array.0.date"
                it shouldContainKey "nested.array.1"
            }
        }
    }

    context("BsonDocument") {
        test("complex nested object is correct") {
            val bsonDocument = BsonDocument(listOf(
                BsonElement("name", BsonString("Bob")),
                BsonElement("nested", BsonDocument(listOf(
                    BsonElement("array", BsonArray(listOf(
                        BsonDocument(listOf(BsonElement("date", BsonString("myDate")))),
                        BsonString("n")
                    ))),
                ))
            )))

           bsonDocument.flatten('.').should {
                println(it)
                it shouldHaveSize 3
                it shouldContainKey "name"
                it shouldContainKey "nested.array.0.date"
                it shouldContainKey "nested.array.1"
            }
        }
    }
})