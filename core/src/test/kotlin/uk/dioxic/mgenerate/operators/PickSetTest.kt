package uk.dioxic.mgenerate.operators

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import uk.dioxic.mgenerate.operators.general.PickSetOperator
import kotlin.math.min
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class PickSetTests : FunSpec({

    val pickSetArb = arbitrary {
        val from = Arb.list(Arb.int(), 0..5).bind()
        val quantity = Arb.int(0..5).bind()
        PickSetOperator(
            from = { from },
            quantity = { quantity },
            slippage = 50
        )
    }
    test("quantity") {
        checkAll(pickSetArb) { operator ->
            val output = operator.invoke()
            output.size shouldBe min(operator.from().distinct().size, operator.quantity())
        }
    }

})