package uk.dioxic.helios.generate

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.bson.Bson
import kotlinx.serialization.bson.buildBsonDocument
import kotlinx.serialization.bson.decodeFromBsonDocument
import kotlinx.serialization.encodeToString
import org.bson.Document
import uk.dioxic.helios.generate.operators.ChooseOperator
import uk.dioxic.helios.generate.operators.IntOperator

class TemplateSerializationTests : FunSpec({

    val bson = Bson { prettyPrint = true }

    val definition = buildBsonDocument {
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

    test("decode") {
        bson.decodeFromBsonDocument<Template>(definition).should {template ->
            template.execution.should {
                it["color"].shouldBeInstanceOf<ChooseOperator>()
                it["height"].shouldBeInstanceOf<IntOperator>()
                it["address"].should { addr ->
                    addr.shouldBeInstanceOf<Document>()
                    addr["city"].shouldBeInstanceOf<ChooseOperator>()
                }
            }
            template.definition shouldBe definition
        }
    }

    test("encode without definition should fail") {
        shouldThrow<IllegalArgumentException> {
            println(bson.encodeToString(Template(Document())))
        }
    }

    test("encode with definition should succeed") {
        val template = Template(Document(), definition)

        shouldNotThrowAny {
            bson.encodeToString(template)
        }
    }

})