package uk.dioxic.helios.generate

import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.maps.shouldContainKeys
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.bson.types.ObjectId
import uk.dioxic.helios.generate.operators.ArrayOperator
import uk.dioxic.helios.generate.operators.NameOperator
import uk.dioxic.helios.generate.operators.ObjectIdOperator

class HydrationTests : FunSpec({

    val json = Json { prettyPrint = true }

    fun hydrateAndPrint(template: Template): Map<String, Any?> {
        println(json.encodeToString(template))
        val map = with(OperatorContext.EMPTY) { template.hydrate() }
        println(map)
        return map
    }

    fun hydrateAndPrint(map: Map<String, Any?>): Map<String, Any?> {
        val res = with(OperatorContext.EMPTY) { map.hydrate() }
        println(res)
        return res
    }

    test("top level operators") {
        val hydrated = hydrateAndPrint(buildTemplate {
            putOperator<NameOperator>("name")
            putOperator<ObjectIdOperator>("oid")
        })

        hydrated.shouldContainKeys("name", "oid")
        hydrated["name"].shouldBeInstanceOf<String>()
        hydrated["oid"].shouldBeInstanceOf<ObjectId>()
    }

    test("nested operators") {
        val hydrated = hydrateAndPrint(buildTemplate {
            putJsonObject("subDoc") {
                putJsonObject("subSubDoc") {
                    putOperator<ObjectIdOperator>("oid")
                }
            }
        })

        hydrated["subDoc"]
            .shouldBeInstanceOf<Map<String, Any>>()
            .get("subSubDoc").shouldBeInstanceOf<Map<String, Any>>()
            .get("oid").shouldBeInstanceOf<ObjectId>()
    }

    test("operators as input to operators") {
        val hydrated = hydrateAndPrint(buildTemplate {
            putOperatorObject<ArrayOperator>("array") {
                putOperator<ObjectIdOperator>("of")
                put("number", 3)
            }
        })

        hydrated["array"]
            .shouldBeInstanceOf<List<*>>()
            .should { l ->
                l.shouldForAll {
                    it.shouldBeInstanceOf<ObjectId>()
                }
                l.distinct().count() shouldBe 3
            }
    }

    test("map with nested operators as input to operators") {
        val hydrated = hydrateAndPrint(buildTemplate {
            putOperatorObject<ArrayOperator>("array") {
                putJsonObject("of") {
                    putOperator<NameOperator>("name")
                    putOperator<ObjectIdOperator>("id")
                }
                put("number", 3)
            }
        })

        hydrated["array"]
            .shouldBeInstanceOf<List<*>>()
            .should { l ->
                l.shouldForAll {
                    it.shouldBeInstanceOf<Map<String, Any?>>().should { m ->
                        m.shouldContainKeys("name", "id")
                        m["name"].shouldBeInstanceOf<String>()
                        m["id"].shouldBeInstanceOf<ObjectId>()
                    }
                }
                l.distinct().count() shouldBe 3
            }
    }

    test("array with nested operators as input to operators") {
        val hydrated = hydrateAndPrint(mapOf(
            "array" to ArrayOperator(
                of = { listOf(ObjectIdOperator(), ObjectIdOperator()) },
                number = { 3 }
            )
        ))

        hydrated["array"]
            .shouldBeInstanceOf<List<*>>()
            .should { l ->
                l.shouldForAll {
                    it.shouldBeInstanceOf<List<*>>().should { vl ->
                        vl.size shouldBe 2
                        vl.shouldForAll { v ->
                            v.shouldBeInstanceOf<ObjectId>()
                        }
                    }
                }
                l.distinct().count() shouldBe 3
            }
    }

})