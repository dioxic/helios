package uk.dioxic.mgenerate.execute.format

import kotlin.time.Duration
import kotlin.time.DurationUnit

fun Duration.toFormatString(): String =
    absoluteValue.toComponents { days, hours, minutes, seconds, nanoseconds ->
        when {
            days != 0L -> toString(DurationUnit.DAYS, 2)
            hours != 0 -> toString(DurationUnit.HOURS, 1)
            minutes != 0 -> toString(DurationUnit.MINUTES, 1)
            seconds != 0 -> toString(DurationUnit.SECONDS, 2)
            nanoseconds >= 1_000_000 -> toString(DurationUnit.MILLISECONDS, 1)
            nanoseconds >= 1_000 -> toString(DurationUnit.MICROSECONDS)
            else -> toString(DurationUnit.NANOSECONDS)
        }
    }