package uk.dioxic.helios.generate

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.bson.Document
import uk.dioxic.helios.generate.operators.ChooseOperator
import uk.dioxic.helios.generate.operators.IntOperator

@OptIn(ExperimentalSerializationApi::class)
class TemplateSerializationTests : FunSpec({

    val json = Json { prettyPrint = true }
    val jsonObject = buildJsonObject {
        putOperatorObject<ChooseOperator>("color") {
            putJsonArray("from") {
                addAll(listOf("blue", "red", "green"))
            }
        }
        putOperator<IntOperator>("height")
        putJsonObject("address") {
            putOperator<ChooseOperator>("city", listOf("london", "new york", "dulwich"))
        }
    }
    val jsonString = json.encodeToString(jsonObject)
    println(jsonString)

    test("decode") {
        json.decodeFromString<Template>(jsonString).should {
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
            json.encodeToString(template)
        }
    }

    test("encode with definition should succeed") {
        val template = Template(emptyMap<String, String>(), jsonObject)

        shouldNotThrowAny {
            json.encodeToString(template)
        }
    }

})