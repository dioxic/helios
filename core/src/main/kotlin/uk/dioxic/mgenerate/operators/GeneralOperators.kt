package uk.dioxic.mgenerate.operators

import uk.dioxic.mgenerate.annotations.Operator

@Operator
fun array(
    of: () -> Any,
    number: () -> Number = {5}
) = generateSequence { of() }
    .take(number().toInt())
    .toList()

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