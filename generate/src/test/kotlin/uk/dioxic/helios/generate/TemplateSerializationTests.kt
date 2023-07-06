package uk.dioxic.helios.generate

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.bson.Bson
import kotlinx.serialization.bson.buildBsonDocument
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.bson.Document
import uk.dioxic.helios.generate.operators.ChooseOperator
import uk.dioxic.helios.generate.operators.IntOperator

class TemplateSerializationTests : FunSpec({

    val bson = Bson { prettyPrint = true }

    val jsonObject = buildBsonDocument {
        putOperatorObject<ChooseOperator>("color") {
            putBsonArray("from") {
                addAll(listOf("blue", "red", "green"))
            }
        }
        putOperator<IntOperator>("height")
        putBsonDocument("address") {
            putOperator<ChooseOperator>("city", listOf("london", "new york", "dulwich"))
        }
    }
    val jsonString = bson.encodeToString(jsonObject)
    println(jsonString)

    test("decode") {
        bson.decodeFromString<Template>(jsonString).should {
            it.shouldBeInstanceOf<Document>()
            it["color"].shouldBeInstanceOf<ChooseOperator>()
            it["height"].shouldBeInstanceOf<IntOperator>()
            it["address"].should { addr ->
                addr.shouldBeInstanceOf<Document>()
                addr["city"].shouldBeInstanceOf<ChooseOperator>()
            }
        }
    }

    test("encode without definition should fail") {
        val template = Template(emptyMap<String, String>())

        shouldThrow<IllegalArgumentException> {
            bson.encodeToString(template)
        }
    }

    test("encode with definition should succeed") {
        val template = Template(emptyMap<String, String>(), jsonObject)

        shouldNotThrowAny {
            bson.encodeToString(template)
        }
    }

})