package uk.dioxic.mgenerate.extensions

import kotlinx.coroutines.flow.Flow

suspend fun Flow<Number>.average(): Double  {
    var sum = 0.0
    var count = 0L
    collect {
        ++count
        sum += it.toDouble()
        if (count < 0L) {
            throw ArithmeticException("Count overflow has happened.")
        }
    }

    return if (count == 0L) Double.NaN else sum / count
}