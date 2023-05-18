package uk.dioxic.mgenerate.operators.numeric

import uk.dioxic.mgenerate.annotations.Alias
import uk.dioxic.mgenerate.operators.Operator
import kotlin.random.Random
import kotlin.random.nextLong

@Alias("long")
class LongOperator(
    val min: () -> Long = { 0 },
    val max: () -> Long = { Long.MAX_VALUE }
) : Operator<Long> {
    override fun invoke() = Random.nextLong(LongRange(min(), max()))
}