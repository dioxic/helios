package uk.dioxic.mgenerate.utils

import kotlinx.coroutines.flow.Flow
import uk.dioxic.mgenerate.operators.toUtcLocalDateTime
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.random.Random

val myLocale: Locale = Locale.ENGLISH

fun <T> Random.Default.nextElement(list: List<T>, weights: List<Int>? = null): T? = when {
    list.isEmpty() -> null
    weights != null -> {
        require(list.size == weights.size) { "length of array and weights must match" }
        list[nextElementIndex(weights)]
    }

    else -> list[nextInt(list.size)]
}

fun Random.Default.nextElementIndex(weights: List<Int>, weightSum: Int? = null): Int {
    require(weights.isNotEmpty()) { "weights input cannot be empty!" }
    var remainingDistance = nextInt(weightSum ?: weights.sum())
    for (i in 0..weights.size) {
        remainingDistance -= weights[i]
        if (remainingDistance < 0) {
            return i
        }
    }
    error("couldn't get an elemement index")
}

fun Random.Default.nextDate(from: LocalDateTime, until: LocalDateTime) =
    nextInstant(from.toInstant(ZoneOffset.UTC), until.toInstant(ZoneOffset.UTC)).toUtcLocalDateTime()

fun Random.Default.nextInstant(from: Instant, until: Instant): Instant =
    Instant.ofEpochMilli(nextLong(from.toEpochMilli(), until.toEpochMilli()))

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