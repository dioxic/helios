package uk.dioxic.mgenerate.operators

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeOneOf
import io.kotest.matchers.collections.shouldBeUnique
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import uk.dioxic.mgenerate.operators.general.ArrayOperator
import uk.dioxic.mgenerate.operators.general.ChooseOperator
import uk.dioxic.mgenerate.operators.general.PickOperator
import uk.dioxic.mgenerate.operators.general.PickSetOperator
import kotlin.math.max
import kotlin.math.min

class CollectionOperatorTests : FunSpec({
    val arrayArb = arbitrary {
        val of = Arb.string(10).bind()
        val number = Arb.int(-1..5).bind()
        ArrayOperator(
            of = { of },
            number = { number }
        )
    }
    val pickSetArb = arbitrary {
        val from = Arb.list(Arb.int(), 0..5).bind()
        val quantity = Arb.int(-1..5).bind()
        PickSetOperator(
            from = { from },
            quantity = { quantity },
            slippage = 50
        )
    }
    val pickArb = arbitrary {
        val array = Arb.list(Arb.string(10), 0..5).bind()
        val element = Arb.int(-1..5).bind()
        PickOperator(
            array = { array },
            element = { element }
        )
    }
    val chooseArb = arbitrary {
        val from = Arb.list(Arb.string(10), 0..5).bind()
        ChooseOperator(
            from = { from },
        )
    }
    test("pick") {
        checkAll(pickArb) { operator ->
            val element = operator.element()
            val array = operator.array()

            if (element < 0) {
                shouldThrowExactly<IllegalArgumentException> {
                    operator.invoke()
                }
            } else {
                val output = operator.invoke()
                when {
                    element < array.size -> output shouldBe array[element]
                    array.isEmpty() -> output.shouldBeNull()
                    else -> output shouldBe array[0]
                }
            }

        }
    }
    test("choose") {
        checkAll(chooseArb) { operator ->
            val from = operator.from()
            val output = operator.invoke()
            if (from.isEmpty()) {
                output.shouldBeNull()
            }
            else {
                output.shouldBeOneOf(from)
            }
        }
    }
    test("array") {
        checkAll(arrayArb) { operator ->
            val of = operator.of()
            val number = operator.number()

            val output = operator.invoke()
            output shouldHaveSize max(number, 0)
            output.all { it == of }
        }
    }
    test("pickSet") {
        checkAll(pickSetArb) { operator ->
            val from = operator.from()
            val quantity = operator.quantity()
            val output = operator.invoke()
            output.shouldBeUnique()
            output shouldHaveSize max(min(from.distinct().size, quantity), 0)
        }
    }
})