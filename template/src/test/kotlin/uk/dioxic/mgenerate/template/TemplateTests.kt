package uk.dioxic.mgenerate.template

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContainKeys
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.bson.types.ObjectId

class TemplateTests : FunSpec({

    context("hydration") {

        test("simple") {
            val hydrated = buildTemplate {
                put("name", "\$name")
                put("oid", "\$objectId")
            }.hydrate()

            hydrated.shouldContainKeys("name", "oid")
            hydrated["name"].shouldBeInstanceOf<String>()
            hydrated["oid"].shouldBeInstanceOf<ObjectId>()
        }

        test("complex") {
            val hydrated = buildTemplate {
                putJsonObject("subDoc") {
                    putJsonObject("subSubDoc") {
                        put("oid", "\$objectId")
                    }
                }
            }.hydrate()

            hydrated["subDoc"]
                .shouldBeInstanceOf<Map<String, Any>>()
                .get("subSubDoc").shouldBeInstanceOf<Map<String, Any>>()
                .get("oid").shouldBeInstanceOf<ObjectId>()
        }
    }

})