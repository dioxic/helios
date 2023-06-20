package uk.dioxic.mgenerate.worker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import uk.dioxic.mgenerate.serialization.FixedRateSerializer
import uk.dioxic.mgenerate.serialization.RateSerializer
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds

@Serializable(RateSerializer::class)
sealed class Rate {
    abstract fun calculateDelay(state: State): Duration
}

@Serializable(FixedRateSerializer::class)
sealed class FixedRate: Rate()

@Serializable
object UnlimitedRate : FixedRate() {
    override fun calculateDelay(state: State): Duration = ZERO
}

@Serializable
@SerialName("fixedTps")
data class TpsRate(
    val tps: Int,
    val fuzzy: Double = 0.0
) : FixedRate() {
    override fun calculateDelay(state: State): Duration =
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
    override fun calculateDelay(state: State): Duration =
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
    override fun calculateDelay(state: State): Duration {
        TODO("Not yet implemented")
    }
}

