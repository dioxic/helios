package uk.dioxic.mgenerate

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import org.bson.Document
import uk.dioxic.mgenerate.operators.IntOperator
import uk.dioxic.mgenerate.operators.general.ChooseOperator
import uk.dioxic.mgenerate.operators.general.PickSetOperator
import uk.dioxic.mgenerate.test.readResource

class OperatorJsonDecode: FunSpec({

    test("decode") {
        val json = readResource("/test.json")
        println("json: $json")

        Template.parse(json).apply {
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

        Template.parse(json).apply {
            shouldBeInstanceOf<Document>()
            get("drivers").shouldBeInstanceOf<PickSetOperator>()
            get("env").shouldBeInstanceOf<ChooseOperator>()
            println("actual: $this")
        }

    }

})