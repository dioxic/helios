package uk.dioxic.helios.generate.operators

import uk.dioxic.helios.generate.Operator
import uk.dioxic.helios.generate.OperatorContext
import uk.dioxic.helios.generate.annotations.Alias
import kotlin.math.abs
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.random.nextLong

@Alias("double")
class DoubleOperator(
    val min: Operator<Double> = Operator { 0.0 },
    val max: Operator<Double> = Operator { Double.MAX_VALUE }
) : Operator<Double> {
    context(OperatorContext)
    override fun invoke(): Double = Random.nextDouble(min(), max())
}

@Alias("abs")
class AbsOperator(
    val input: Operator<Number?>
) : Operator<Number?> {
    context(OperatorContext)
    override fun invoke(): Number? = when (val i = input()) {
        is Double -> abs(i)
        is Int -> abs(i)
        is Float -> abs(i)
        is Long -> abs(i)
        null -> null
        else -> error("abs does not support ${i::class}")
    }
}

@Alias("int")
class IntOperator(
    val min: Operator<Int> = Operator { 0 },
    val max: Operator<Int> = Operator { Int.MAX_VALUE }
) : Operator<Int> {
    context(OperatorContext)
    override fun invoke(): Int = Random.nextInt(min()..max())
}

@Alias("long")
class LongOperator(
    val min: Operator<Long> = Operator { 0 },
    val max: Operator<Long> = Operator { Long.MAX_VALUE }
) : Operator<Long> {
    context(OperatorContext)
    override fun invoke() = Random.nextLong(LongRange(min(), max()))
}