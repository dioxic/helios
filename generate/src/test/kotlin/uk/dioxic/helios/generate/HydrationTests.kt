package uk.dioxic.helios.generate

import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.maps.shouldContainKeys
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.bson.types.ObjectId
import uk.dioxic.helios.generate.operators.*
import uk.dioxic.helios.generate.test.hydrateAndPrint

class HydrationTests : FunSpec({

    test("top level operators") {
        val hydrated = mapOf(
            "name" to NameOperator(),
            "oid" to ObjectIdOperator()
        ).hydrateAndPrint()

        hydrated.shouldContainKeys("name", "oid")
        hydrated["name"].shouldBeInstanceOf<String>()
        hydrated["oid"].shouldBeInstanceOf<ObjectId>()
    }

    test("nested operators") {
        val hydrated = mapOf(
            "subDoc" to mapOf(
                "subSubDoc" to mapOf(
                    "oid" to ObjectIdOperator()
                )
            )
        ).hydrateAndPrint()

        hydrated["subDoc"].shouldBeInstanceOf<Map<String, Any>>().should { subDoc ->
            subDoc["subSubDoc"].shouldBeInstanceOf<Map<String, Any>>().should {
                it["oid"].shouldBeInstanceOf<ObjectId>()
            }
        }
    }

    test("operators as input to operators") {
        val hydrated = mapOf(
            "array" to ArrayOperator(
                of = { ObjectIdOperator() },
                number = { 3 }
            )
        ).hydrateAndPrint()

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
        val hydrated = mapOf(
            "array" to ArrayOperator(
                of = { mapOf("name" to NameOperator(), "id" to ObjectIdOperator()) },
                number = { 3 }
            )
        ).hydrateAndPrint()

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
        val hydrated = mapOf(
            "array" to ArrayOperator(
                of = { listOf(ObjectIdOperator(), ObjectIdOperator()) },
                number = { 3 }
            )
        ).hydrateAndPrint()

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

    test("root operator") {
        val personMap = mapOf(
            "type" to "person",
            "height" to 12
        )
        val orgMap = mapOf(
            "type" to "org",
            "orgId" to 123
        )
        val hydrated = mapOf(
            getOperatorKey<RootOperator>() to ChooseOperator(
                from = { listOf(personMap, orgMap) }
            )
        ).hydrateAndPrint()

        hydrated.shouldContainKeys("type")
    }

})