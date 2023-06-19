package uk.dioxic.mgenerate.worker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Serializable(RateSerializer::class)
sealed class Rate {
    abstract fun calculateDelay(state: ExecutionState): Duration
}

@Serializable(FixedRateSerializer::class)
sealed class FixedRate: Rate()

@Serializable
object Unlimited : FixedRate() {
    override fun calculateDelay(state: ExecutionState): Duration = ZERO
}

@Serializable
@SerialName("fixedTps")
data class TpsRate(
    val tps: Int,
    val fuzzy: Double = 0.0
) : FixedRate() {
    override fun calculateDelay(state: ExecutionState): Duration =
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
    override fun calculateDelay(state: ExecutionState): Duration =
        when (fuzzy) {
            0.0 -> period
            else -> period + (period * Random.nextDouble(-fuzzy, fuzzy))
        }
}

@Serializable
@SerialName("ramped")
data class RampedRate(
    val from: FixedRate,
    val to: FixedRate = Unlimited,
    val rampDuration: Duration
) : Rate() {
    override fun calculateDelay(state: ExecutionState): Duration {
        TODO("Not yet implemented")
    }
}

suspend fun delay(context: ExecutionContext) {
//    TODO()
    val delay = 1.seconds //context.workload.rate.calculateDelay(context.state)
    when {
        delay == ZERO -> return
        delay < 100.milliseconds -> {
            val deadline = System.nanoTime() + delay.inWholeNanoseconds
            while (System.nanoTime() < deadline){}
        }
        else -> kotlinx.coroutines.delay(delay)
    }
}