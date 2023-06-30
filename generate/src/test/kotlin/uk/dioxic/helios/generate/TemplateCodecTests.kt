package uk.dioxic.helios.generate

import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKeys
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import uk.dioxic.helios.generate.operators.ArrayOperator
import uk.dioxic.helios.generate.operators.NameOperator
import uk.dioxic.helios.generate.operators.ObjectIdOperator
import uk.dioxic.helios.generate.test.shouldBeWrapped

class TemplateCodecTests : FunSpec({

    val json = Json { prettyPrint = true }

    fun printJson(template: Template) {
        println(json.encodeToString(template))
    }

    context("decode") {
        test("top level operators") {
            val template = buildTemplate {
                putOperator<NameOperator>("name")
                putOperator<ObjectIdOperator>("oid")
            }

            printJson(template)

            template["name"].shouldBeInstanceOf<NameOperator>()
            template["oid"].shouldBeInstanceOf<ObjectIdOperator>()
        }

        test("nested operators") {
            val template = buildTemplate {
                putJsonObject("subDoc") {
                    putJsonObject("subSubDoc") {
                        putOperator<ObjectIdOperator>("oid")
                    }
                }
            }

            printJson(template)

            template["subDoc"].shouldBeInstanceOf<Map<String, Any>>().should { subDoc ->
                subDoc["subSubDoc"].shouldBeInstanceOf<Map<String, Any>>().should { subSubDoc ->
                    subSubDoc["oid"].shouldBeInstanceOf<ObjectIdOperator>()
                }
            }
        }

        test("operators as input to operators") {
            val template = buildTemplate {
                putOperatorObject<ArrayOperator>("array") {
                    putOperator<ObjectIdOperator>("of")
                    put("number", 3)
                }
            }

            printJson(template)

            template["array"]
                .shouldBeInstanceOf<ArrayOperator>()
                .should { array ->
                    array.of.shouldBeInstanceOf<ObjectIdOperator>()
                    array.number.shouldBeWrapped<Int>() shouldBe 3
                }
        }

        test("map with nested operators as input to operators") {
            val template = buildTemplate {
                putOperatorObject<ArrayOperator>("array") {
                    putJsonObject("of") {
                        putOperator<NameOperator>("name")
                        putOperator<ObjectIdOperator>("id")
                    }
                    put("number", 3)
                }
            }

            printJson(template)

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

        test("array with nested operators as input to operators") {
            val template = buildTemplate {
                putOperatorObject<ArrayOperator>("array") {
                    putJsonArray("of") {
                        addOperator<ObjectIdOperator>()
                        addOperator<ObjectIdOperator>()
                    }
                    put("number", 3)
                }
            }

            printJson(template)

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

})