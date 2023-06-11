package uk.dioxic.mgenerate.extensions

import uk.dioxic.mgenerate.worker.Rate
import java.util.*

val myLocale: Locale = Locale.ENGLISH

inline val Int.tps get() = Rate.of(this)

fun Iterable<Number>.average(): Double {
    var sum: Double = 0.0
    var count: Int = 0
    for (element in this) {
        sum += element.toDouble()
        count++
        if (count < 0) {
            throw ArithmeticException("Count overflow has happened.")
        }
    }
    return if (count == 0) Double.NaN else sum / count
}

