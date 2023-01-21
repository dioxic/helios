package uk.dioxic.mgenerate.operators

import uk.dioxic.mgenerate.AnyFunction
import uk.dioxic.mgenerate.annotations.Operator
import uk.dioxic.mgenerate.average
import kotlin.random.Random

@Operator
fun array(
    of: () -> Any,
    number: () -> Number = { 5 }
) = generateSequence { of() }
    .take(number().toInt())
    .toList()

@Operator("avg", "average")
fun avg(input: List<Number>) =
    input.average()

@Operator
fun boolean() =
    Random.nextBoolean()

fun <T:Any> choose(
    from: List<T>,
    weights: List<Int>
): T {
    require(from.size == weights.size) { "length of array and weights must match" }
    val weightedList = mutableListOf<T>()

    from.forEachIndexed{ i, f ->
        repeat(weights[i]) {
            weightedList.add(f)
        }
    }

    return weightedList[Random.nextInt(weightedList.size)]
}

@Operator("choose")
public class ChooseOperator<T:Any>(
    public val from: () -> List<T>,
    public val weights: () -> List<Int>,
) : AnyFunction {
    public override fun invoke(): T = choose(from = from(), weights = weights())
}

@Operator("inc", "increment")
fun inc(input: Number, step: Number): Number =
    when (step) {
        is Double -> input.toDouble() + step
        is Float -> input.toFloat() + step
        else -> when (input) {
            is Long -> input + step.toLong()
            is Int -> input + step.toInt()
            is Double -> input + step.toDouble()
            is Float -> input + step.toFloat()
            else -> input.toInt() + step.toInt()
        }
    }