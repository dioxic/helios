package uk.dioxic.helios.generate.operators

import org.bson.BsonTimestamp
import uk.dioxic.helios.generate.Operator
import uk.dioxic.helios.generate.OperatorContext
import uk.dioxic.helios.generate.annotations.Alias
import uk.dioxic.helios.generate.extensions.myLocale
import uk.dioxic.helios.generate.extensions.nextInstant
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
    val min: Operator<Instant>,
    val max: Operator<Instant>
) : Operator<Instant> {

    context(OperatorContext)
    override fun invoke(): Instant =
        Random.nextInstant(min(), max())

}

@Alias("now")
class NowOperator : Operator<Instant> {

    context(OperatorContext)
    override fun invoke(): Instant = Instant.now()
}

@Alias("dayOfMonth")
class DayOfMonthOperator(
    val date: Operator<Instant> = Operator { Instant.now() }
) : Operator<Int> {

    context(OperatorContext)
    override fun invoke(): Int =
        date().toUtcLocalDateTime().dayOfMonth
}

@Alias("dayOfWeek")
class DayOfWeekOperator(
    val date: Operator<Instant> = Operator { Instant.now() },
    val format: Operator<OutputFormat> = Operator { OutputFormat.NUMERIC }
) : Operator<Any> {

    context(OperatorContext)
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
    val date: Operator<Instant> = Operator { Instant.now() }
) : Operator<Int> {

    context(OperatorContext)
    override fun invoke(): Int = date().toUtcLocalDateTime().dayOfYear
}

@Alias("epoch")
class EpochOperator(
    val date: Operator<Instant> = Operator { Instant.now() },
    val unit: Operator<ChronoUnit> = Operator { ChronoUnit.MILLIS }
) : Operator<Long> {

    private val zeroEpoch = Instant.EPOCH


    context(OperatorContext)
    override fun invoke(): Long = zeroEpoch.until(date(), unit())
}

@Alias("hour")
class HourOperator(
    val date: Operator<Instant> = Operator { Instant.now() }
) : Operator<Int> {

    context(OperatorContext)
    override fun invoke(): Int = date().toUtcLocalDateTime().hour
}

@Alias("minute")
class MinuteOperator(
    val date: Operator<Instant> = Operator { Instant.now() }
) : Operator<Int> {

    context(OperatorContext)
    override fun invoke(): Int = date().toUtcLocalDateTime().minute
}

@Alias("second")
class SecondOperator(
    val date: Operator<Instant> = Operator { Instant.now() }
) : Operator<Int> {

    context(OperatorContext)
    override fun invoke(): Int =
        date().toUtcLocalDateTime().second
}

@Alias("year")
class YearOperator(
    val date: Operator<Instant> = Operator { Instant.now() }
) : Operator<Int> {

    context(OperatorContext)
    override fun invoke(): Int =
        date().toUtcLocalDateTime().year
}

@Alias("year")
class TimestampOperator(
    val t: Operator<Int>,
    val i: Operator<Int> = Operator { 0 },
) : Operator<BsonTimestamp> {

    context(OperatorContext)
    override fun invoke(): BsonTimestamp =
        BsonTimestamp(t(), i())
}