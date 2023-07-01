package uk.dioxic.helios.generate

import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldContainKeys
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.*
import uk.dioxic.helios.generate.codecs.TemplateCodec
import uk.dioxic.helios.generate.operators.*
import uk.dioxic.helios.generate.test.shouldBeWrapped

class TemplateCodecTests : FunSpec({

    context("decode") {

        fun decodeAndPrint(jsonObject: JsonObject): Template =
            TemplateCodec().decode(jsonObject).also {
                println(it)
            }

        test("top level operators") {
            val jsonObject = buildJsonObject {
                putOperator<NameOperator>("name")
                putOperator<ObjectIdOperator>("oid")
            }

            decodeAndPrint(jsonObject).should { template ->
                template.shouldContainKeys("name", "oid")
                template["name"].shouldBeInstanceOf<NameOperator>()
                template["oid"].shouldBeInstanceOf<ObjectIdOperator>()
            }

        }

        test("nested operators") {
            val jsonObject = buildJsonObject {
                putJsonObject("subDoc") {
                    putJsonObject("subSubDoc") {
                        putOperator<ObjectIdOperator>("oid")
                    }
                }
            }

            decodeAndPrint(jsonObject).should { template ->
                template shouldContainKey "subDoc"
                template["subDoc"].shouldBeInstanceOf<Map<String, Any>>().should { subDoc ->
                    subDoc shouldContainKey "subSubDoc"
                    subDoc["subSubDoc"].shouldBeInstanceOf<Map<String, Any>>().should { subSubDoc ->
                        subSubDoc shouldContainKey "oid"
                        subSubDoc["oid"].shouldBeInstanceOf<ObjectIdOperator>()
                    }
                }
            }

        }

        test("operators as input to operators") {
            val jsonObject = buildJsonObject {
                putOperatorObject<ArrayOperator>("array") {
                    putOperator<ObjectIdOperator>("of")
                    put("number", 3)
                }
            }

            decodeAndPrint(jsonObject).should { template ->
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
            val jsonObject = buildJsonObject {
                putOperatorObject<ArrayOperator>("array") {
                    putJsonObject("of") {
                        putOperator<NameOperator>("name")
                        putOperator<ObjectIdOperator>("id")
                    }
                    put("number", 3)
                }
            }

            decodeAndPrint(jsonObject).should { template ->
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
            val jsonObject = buildJsonObject {
                putOperatorObject<ArrayOperator>("array") {
                    putJsonArray("of") {
                        addOperator<ObjectIdOperator>()
                        addOperator<ObjectIdOperator>()
                    }
                    put("number", 3)
                }
            }

            decodeAndPrint(jsonObject).should { template ->
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
        fun encodeAndPrint(template: Template): JsonObject =
            template.toJson().let {
                println(it)
                Json.decodeFromString<JsonObject>(it)
            }

        test("root operator") {
            val personMap = mapOf(
                "type" to "person",
                "height" to 12
            )
            val orgMap = mapOf(
                "type" to "org",
                "orgId" to 123
            )
            val template = Template(mapOf(
                getOperatorKey<RootOperator>() to ChooseOperator(
                    from = { listOf(personMap, orgMap) }
                )
            ))

            encodeAndPrint(template).should {
                it shouldContainKey "type"
                it["type"].shouldBeInstanceOf<String>()
            }
        }
    }

})