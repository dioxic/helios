package uk.dioxic.mgenerate.operators.numeric

import uk.dioxic.mgenerate.annotations.Alias
import uk.dioxic.mgenerate.operators.Operator
import kotlin.random.Random
import kotlin.random.nextInt

@Alias("int")
class IntOperator(
    val min: () -> Int = { 0 },
    val max: () -> Int = { Int.MAX_VALUE }
) : Operator<Int> {
    override fun invoke(): Int = Random.nextInt(min()..max())
}