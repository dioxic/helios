package uk.dioxic.helios.generate.extensions

import uk.dioxic.helios.generate.operators.toUtcLocalDateTime
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.random.Random

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