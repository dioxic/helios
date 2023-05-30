package uk.dioxic.mgenerate

import assertk.all
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.key
import org.bson.Document
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import uk.dioxic.mgenerate.Template
import uk.dioxic.mgenerate.operators.general.ChooseOperator
import uk.dioxic.mgenerate.operators.general.PickSetOperator
import uk.dioxic.mgenerate.operators.IntOperator
import uk.dioxic.mgenerate.test.readResource

class OperatorJsonDecode {

    @Test
    fun decode() {
        val json = readResource("/test.json")
        val actual = assertDoesNotThrow { Template.parse(json) }

        println("json: $json")
        println(actual)

        assertThat(actual, "document")
            .isInstanceOf(Document::class)
            .all {
                key("color").isInstanceOf(ChooseOperator::class)
                key("height").isInstanceOf(IntOperator::class)
                key("address")
                    .isInstanceOf(Document::class)
                    .key("city").isInstanceOf(ChooseOperator::class)
            }
    }

    @Test
    fun encodeToJson() {
        val json = readResource("/test3.json")
        val template = Template.parse(json)

        assertThat(template, "document")
            .isInstanceOf(Document::class)
            .all {
                key("drivers").isInstanceOf(PickSetOperator::class)
                key("env").isInstanceOf(ChooseOperator::class)
            }

        val actual = template.toJson()

//        println("json: $json")
        println("actual: $actual")

    }

}