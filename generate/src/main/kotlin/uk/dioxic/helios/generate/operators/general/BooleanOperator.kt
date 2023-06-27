package uk.dioxic.helios.generate.operators.general

import uk.dioxic.helios.generate.annotations.Alias
import uk.dioxic.helios.generate.operators.Operator
import kotlin.random.Random

@Alias("bool")
class BooleanOperator(
    val probability: () -> Double = { 0.5 }
) : Operator<Boolean> {

    override fun invoke(): Boolean =
        Random.nextDouble(0.0, probability()) > 0.5
}