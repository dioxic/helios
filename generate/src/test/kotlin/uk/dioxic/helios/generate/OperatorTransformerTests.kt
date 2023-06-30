package uk.dioxic.helios.generate

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.types.shouldBeInstanceOf
import org.bson.Document
import uk.dioxic.helios.generate.operators.ChooseOperator
import uk.dioxic.helios.generate.test.withEmptyContext

class OperatorTransformerTests: FunSpec({

    val colours = listOf("blue", "red", "green")

    val doc = Document(
        mapOf(
            "\$choose" to mapOf(
                "from" to colours
            )
        )
    )

    test("decode") {
        withEmptyContext {
            OperatorTransformer.transform(doc).let {
                it.shouldBeInstanceOf<ChooseOperator>()
                it.invoke().shouldBeIn(*colours.toTypedArray())
            }
        }
    }

})