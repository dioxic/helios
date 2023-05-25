package uk.dioxic.mgenerate.utils

import kotlinx.datetime.UtcOffset
import uk.dioxic.mgenerate.operators.toUtcLocalDateTime
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale
import kotlin.random.Random

val myLocale: Locale = Locale.ENGLISH

fun Random.Default.nextElement(list: List<Any>, weights: List<Int>? = null): Any? {
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
