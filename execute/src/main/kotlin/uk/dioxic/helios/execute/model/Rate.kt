@file:OptIn(ExperimentalTime::class)

package uk.dioxic.helios.execute.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import uk.dioxic.helios.execute.serialization.BsonFixedRateSerializer
import uk.dioxic.helios.execute.serialization.BsonRateSerializer
import kotlin.math.min
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@Serializable(BsonRateSerializer::class)
sealed class Rate {
    context(ExecutionContext)
    abstract fun calculateDelay(): Duration
}

@Serializable(BsonFixedRateSerializer::class)
sealed class FixedRate: Rate() {
    abstract fun calculateBaseDelay(): Duration
}

@Serializable
object UnlimitedRate : FixedRate() {
    override fun calculateBaseDelay(): Duration = ZERO
    override fun calculateDelay(): Duration = ZERO
}

@Serializable
@SerialName("fixedTps")
data class TpsRate(
    val tps: Int,
    val fuzzy: Double = 0.0
) : FixedRate() {
    init {
        require(fuzzy >= 0.0 && fuzzy < 1.0) {
            "fuzzy must be between 0.0 and 1.0"
        }
    }

    override fun calculateBaseDelay(): Duration = 1.seconds.div(tps)

    override fun calculateDelay(): Duration =
        1.seconds.div(
            when (fuzzy) {
                0.0 -> tps.toDouble()
                else -> tps + (tps * Random.nextDouble(-fuzzy, fuzzy))
            }
        )
    }

@Serializable
@SerialName("fixedPeriod")
data class PeriodRate(
    val period: Duration,
    val fuzzy: Double = 0.0
) : FixedRate() {
    init {
        require(fuzzy >= 0.0 && fuzzy < 1.0) {
            "fuzzy must be between 0.0 and 1.0"
        }
    }

    override fun calculateBaseDelay(): Duration = period

    override fun calculateDelay(): Duration =
        when (fuzzy) {
            0.0 -> period
            else -> period + (period * Random.nextDouble(-fuzzy, fuzzy))
        }
}

@Serializable
@SerialName("ramped")
data class RampedRate(
    val from: FixedRate,
    val to: FixedRate = UnlimitedRate,
    val rampDuration: Duration
) : Rate() {
    init {
        require(rampDuration.isPositive()) {
            "rampDuration must be positive"
        }
    }
    private val fromRate = from.calculateBaseDelay()
    private val rateOffset = fromRate - to.calculateBaseDelay()

    context(ExecutionContext)
    override fun calculateDelay(): Duration {
        val currentOffset = startTime.elapsedNow()
        val rampPercentage = min(currentOffset / rampDuration, 1.0)

        return fromRate - rateOffset.times(rampPercentage)
    }
}

