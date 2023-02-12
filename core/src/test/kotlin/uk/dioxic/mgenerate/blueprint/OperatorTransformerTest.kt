package uk.dioxic.mgenerate.blueprint

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isSuccess
import org.bson.Document
import org.junit.jupiter.api.Test

class OperatorTransformerTest {

    @Test
    fun decode() {
        val doc = Document(
            mapOf(
                "\$choose" to mapOf(
                    "from" to listOf("blue", "red", "green")
                )
            )
        )

        val transformer = OperatorTransformer()
        assertThat { transformer.transform(doc) }
            .isSuccess()
            .isInstanceOf(ChooseOperator::class)

    }

}