package uk.dioxic.mgenerate.template

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.types.shouldBeInstanceOf
import org.bson.Document
import uk.dioxic.mgenerate.template.operators.general.ChooseOperator

class OperatorTransformerTest: FunSpec({

    val colours = listOf("blue", "red", "green")

    val doc = Document(
        mapOf(
            "\$choose" to mapOf(
                "from" to colours
            )
        )
    )

    test("decode") {
        OperatorTransformer().transform(doc).let {
            it.shouldBeInstanceOf<ChooseOperator>()
            it.invoke().shouldBeIn(*colours.toTypedArray())
        }
    }

})