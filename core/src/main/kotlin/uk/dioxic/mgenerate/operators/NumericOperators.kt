package uk.dioxic.mgenerate.operators

import uk.dioxic.mgenerate.annotations.Alias
import uk.dioxic.mgenerate.annotations.Operator
import kotlin.math.abs
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.random.nextLong

@Alias("double")
class DoubleOperator(
    val min: () -> Double = { 0.0 },
    val max: () -> Double = { Double.MAX_VALUE }
) : Operator<Double> {
    override fun invoke(): Double = Random.nextDouble(min(), max())
}

@Alias("abs")
class AbsOperator(
    val input: () -> Number?
) : Operator<Number?> {
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
    val min: () -> Int = { 0 },
    val max: () -> Int = { Int.MAX_VALUE }
) : Operator<Int> {
    override fun invoke(): Int = Random.nextInt(min()..max())
}

@Alias("long")
class LongOperator(
    val min: () -> Long = { 0 },
    val max: () -> Long = { Long.MAX_VALUE }
) : Operator<Long> {
    override fun invoke() = Random.nextLong(LongRange(min(), max()))
}