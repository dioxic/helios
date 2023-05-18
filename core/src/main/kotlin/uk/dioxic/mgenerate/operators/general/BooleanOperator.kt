package uk.dioxic.mgenerate.operators.general

import uk.dioxic.mgenerate.annotations.Alias
import uk.dioxic.mgenerate.operators.Operator
import kotlin.random.Random

@Alias("bool")
class BooleanOperator(
    val probability: () -> Double = { 0.5 }
) : Operator<Boolean> {

    override fun invoke(): Boolean =
        Random.nextDouble(0.0, probability()) > 0.5
}