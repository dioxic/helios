package uk.dioxic.mgenerate.operators.numeric

import uk.dioxic.mgenerate.annotations.Alias
import uk.dioxic.mgenerate.operators.Operator
import kotlin.random.Random
import kotlin.random.nextInt

@Alias("double")
class DoubleOperator(
    val min: () -> Double = { 0.0 },
    val max: () -> Double = { Double.MAX_VALUE }
) : Operator<Double> {
    override fun invoke(): Double = Random.nextDouble(min(), max())
}