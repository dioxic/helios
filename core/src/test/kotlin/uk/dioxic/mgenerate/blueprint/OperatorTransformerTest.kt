package uk.dioxic.mgenerate.blueprint

import assertk.assertThat
import assertk.assertions.isIn
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import org.bson.Document
import org.junit.jupiter.api.Test
import uk.dioxic.mgenerate.OperatorTransformer
import uk.dioxic.mgenerate.operators.general.ChooseOperator

class OperatorTransformerTest {

    private val colours = listOf("blue", "red", "green")

    private val doc = Document(
        mapOf(
            "\$choose" to mapOf(
                "from" to colours
            )
        )
    )

    @Test
    fun decode() {

        val transformer = OperatorTransformer()
        assertThat(transformer.transform(doc), name = "transform")
            .isInstanceOf(ChooseOperator::class)
            .transform("execute") { it.invoke() }
            .isNotNull()
            .isIn(*colours.toTypedArray())

    }

}