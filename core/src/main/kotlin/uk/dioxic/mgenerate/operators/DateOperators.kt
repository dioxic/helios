package uk.dioxic.mgenerate.operators

import org.bson.BsonTimestamp
import uk.dioxic.mgenerate.annotations.Alias
import uk.dioxic.mgenerate.annotations.Operator
import uk.dioxic.mgenerate.extensions.myLocale
import uk.dioxic.mgenerate.extensions.nextInstant
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import kotlin.random.Random

enum class OutputFormat {
    STRING,
    NUMERIC
}

fun Instant.toUtcLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(this, ZoneOffset.UTC)

@Alias("date", "dt")
class DateOperator(
    val min: () -> Instant,
    val max: () -> Instant
) : Operator<Instant> {
    override fun invoke(): Instant =
        Random.nextInstant(min(), max())
}

@Alias("now")
class NowOperator : Operator<Instant> {
    override fun invoke(): Instant = Instant.now()
}

@Alias("dayOfMonth")
class DayOfMonthOperator(
    val date: () -> Instant = { Instant.now() }
) : Operator<Int> {
    override fun invoke(): Int =
        date().toUtcLocalDateTime().dayOfMonth
}

@Alias("dayOfWeek")
class DayOfWeekOperator(
    val date: () -> Instant = { Instant.now() },
    val format: () -> OutputFormat = { OutputFormat.NUMERIC }
) : Operator<Any> {
    override fun invoke(): Any {
        val dayOfWeek = date().toUtcLocalDateTime().dayOfWeek
        return when (format()) {
            OutputFormat.STRING -> dayOfWeek.getDisplayName(TextStyle.FULL, myLocale)
            OutputFormat.NUMERIC -> dayOfWeek.value
        }
    }
}

@Alias("dayOfYear")
class DayOfYearOperator(
    val date: () -> Instant = { Instant.now() }
) : Operator<Int> {
    override fun invoke(): Int = date().toUtcLocalDateTime().dayOfYear
}

@Alias("epoch")
class EpochOperator(
    val date: () -> Instant = { Instant.now() },
    val unit: () -> ChronoUnit = { ChronoUnit.MILLIS }
) : Operator<Long> {

    private val zeroEpoch = Instant.EPOCH

    override fun invoke(): Long = zeroEpoch.until(date(), unit())
}

@Alias("hour")
class HourOperator(
    val date: () -> Instant = { Instant.now() }
) : Operator<Int> {
    override fun invoke(): Int = date().toUtcLocalDateTime().hour
}

@Alias("minute")
class MinuteOperator(
    val date: () -> Instant = { Instant.now() }
) : Operator<Int> {
    override fun invoke(): Int = date().toUtcLocalDateTime().minute
}

@Alias("second")
class SecondOperator(
    val date: () -> Instant = { Instant.now() }
) : Operator<Int> {
    override fun invoke(): Int =
        date().toUtcLocalDateTime().second
}

@Alias("year")
class YearOperator(
    val date: () -> Instant = { Instant.now() }
) : Operator<Int> {
    override fun invoke(): Int =
        date().toUtcLocalDateTime().year
}

@Alias("year")
class TimestampOperator(
    val t: () -> Int,
    val i: () -> Int = { 0 },
) : Operator<BsonTimestamp> {
    override fun invoke(): BsonTimestamp =
        BsonTimestamp(t(), i())
}