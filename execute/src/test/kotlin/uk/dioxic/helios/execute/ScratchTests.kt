package uk.dioxic.helios.execute

import io.kotest.core.spec.style.FunSpec
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.bson.BsonArray
import org.bson.BsonDocument
import org.bson.BsonString
import uk.dioxic.helios.execute.model.Benchmark
import uk.dioxic.helios.execute.test.readResource
import uk.dioxic.helios.generate.ejson.Bson
import uk.dioxic.helios.generate.ejson.decodeFromBsonDocument
import uk.dioxic.helios.generate.ejson.encodeToBsonDocument

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

class ScratchTests: FunSpec({

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
})