package uk.dioxic.helios.execute

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.bson.Bson
import kotlinx.serialization.bson.buildBsonDocument
import kotlinx.serialization.bson.decodeFromBsonDocument
import kotlinx.serialization.bson.encodeToBsonDocument
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.bson.BsonArray
import org.bson.BsonDocument
import org.bson.BsonString
import uk.dioxic.helios.execute.model.Benchmark
import uk.dioxic.helios.execute.test.readResource
import uk.dioxic.helios.generate.OperatorContext
import uk.dioxic.helios.generate.Template
import uk.dioxic.helios.generate.operators.ChooseOperator
import uk.dioxic.helios.generate.operators.NameOperator
import uk.dioxic.helios.generate.putOperatorObject

@Serializable
data class Person(
    val name: String,
    val age: Int,
    val address: Address,
    @Contextual val bsonString: BsonString,
    @Contextual val bsonArray: BsonArray,
    @Contextual val bsonDocument: BsonDocument,
)

@Serializable
data class Address(
    val city: String
)

class ScratchTests : FunSpec({

    val animalList = BsonArray(listOf(BsonString("fish"), BsonString("duck")))
    val person = Person(
        name = "Bob",
        age = 25,
        bsonString = BsonString("Halibut"),
        bsonArray = animalList,
        bsonDocument = BsonDocument("animals", animalList),
        address = Address("London")
    )

    val bson = Bson {
        prettyPrint = true
    }

    test("json test") {
        val json = bson.encodeToString(person)
        println(json)

        val person2 = bson.decodeFromString<Person>(json)
        println(person2)
    }

    test("bsonvalue test") {
        val doc = bson.encodeToBsonDocument(person)
        println(doc)

        val person2 = bson.decodeFromBsonDocument<Person>(doc)
        println(person2)
    }

    test("benchmark test") {
        val json = readResource("/benchmark.json")

        val benchmark = bson.decodeFromString<Benchmark>(json)
//        println(benchmark)

        val str = bson.encodeToString(benchmark)
        println(str)

    }

    test("template test") {
        val template = Template(
            mapOf("person" to mapOf("name" to ChooseOperator(
                from = { listOf(NameOperator(), 123) }
            ))),
            mapOf("name" to mapOf("\$choose" to mapOf("from" to listOf("\$name", 123))))
        )

        val str = bson.encodeToString(template)

        println(str)

        val decoded = bson.decodeFromString<Template>(str)

        decoded.map["name"].should { name ->
            name.shouldNotBeNull()
            name.shouldBeInstanceOf<ChooseOperator>().should { choose ->
                with(OperatorContext.EMPTY) {
                    choose.from.invoke().should { from ->
                        from shouldHaveSize 2
                        from[0].shouldBeInstanceOf<NameOperator>()
                        from[1] shouldBe 123
                    }
                }
            }
        }

        println(decoded)
    }

    test("new template") {
        val templateBson = buildBsonDocument {
            put("name", "bob")
            putBsonDocument("address") {
                putOperatorObject<ChooseOperator>("city") {
                    putBsonArray("from") {
                        add("London")
                        add("Belfast")
                        add("Dublin")
                    }
                }
            }
        }

        println("bson: $templateBson")

        val template = Bson.decodeFromBsonDocument<NewTemplate>(templateBson)

        println("Template: $template")

        val json = Bson.encodeToString(template)

        println("json: $json")

    }
})

