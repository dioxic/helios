package uk.dioxic.helios.generate

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json
import org.bson.Document
import uk.dioxic.helios.generate.operators.ChooseOperator
import uk.dioxic.helios.generate.operators.IntOperator
import uk.dioxic.helios.generate.operators.PickSetOperator
import uk.dioxic.helios.generate.test.readResource

class OperatorEncodingTest: FunSpec({

    test("decode") {
        val json = readResource("/test.json")
        println("json: $json")

        Json.decodeFromString<Template>(json).apply {
            shouldBeInstanceOf<Document>()
            get("color").shouldBeInstanceOf<ChooseOperator>()
            get("height").shouldBeInstanceOf<IntOperator>()
            get("address")
                .shouldBeInstanceOf<Document>()
                .get("city").shouldBeInstanceOf<ChooseOperator>()
            println(this)
        }
    }

    test("encodeToJson") {
        val json = readResource("/test3.json")
        println(json)

        Json.decodeFromString<Template>(json).apply {
            shouldBeInstanceOf<Document>()
            get("drivers").shouldBeInstanceOf<PickSetOperator>()
            get("env").shouldBeInstanceOf<ChooseOperator>()
            println("actual: $this")
        }

    }

})