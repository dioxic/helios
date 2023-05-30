package uk.dioxic.mgenerate.utils

import org.apache.commons.math3.stat.StatUtils
import uk.dioxic.mgenerate.operators.toUtcLocalDateTime
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

val myLocale: Locale = Locale.ENGLISH

fun <T> Random.Default.nextElement(list: List<T>, weights: List<Int>? = null): T? {
    when {
        list.isEmpty() -> return null
        weights != null -> {
            require(list.size == weights.size) { "length of array and weights must match" }
            var remainingDistance = nextInt(weights.sum())

            for (i in 0..list.size) {
                remainingDistance -= weights[i]
                if (remainingDistance < 0) {
                    return list[i]
                }
            }
            error("couldn't pick a weighted item!")
        }

        else -> return list[nextInt(list.size)]
    }
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

fun Iterable<Duration>.percentile(percentile: Double) = asSequence().percentile(percentile)

fun Sequence<Duration>.percentile(percentile: Double) =
    StatUtils.percentile(
        map { it.toDouble(DurationUnit.MILLISECONDS) }
            .toList()
            .toDoubleArray(),
        percentile
    ).toDuration(DurationUnit.MILLISECONDS)