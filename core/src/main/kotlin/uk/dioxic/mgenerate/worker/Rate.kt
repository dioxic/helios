package uk.dioxic.mgenerate.worker

import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

sealed interface IRate {
    suspend fun delay(startMillis: Long)
//
//    companion object {
//        val UNLIMITED = UnlimitedRate
//
//        fun create(tps: Int): FixedRate {
//            require(tps > 1) { "tps must be positive" }
//            return FixedRate(1.seconds.div(tps))
//        }
//
//        fun create(delay: Duration): FixedRate =
//            FixedRate(delay)
//    }
}

object UnlimitedRate: IRate {
    override suspend fun delay(startMillis: Long) {}
}

data class FixedRate(val delay: Duration): IRate {
    override suspend fun delay(startMillis: Long) {
        doDelay(delay)
    }
}

data class RampRate(val from: Duration, val to: Duration = ZERO, val rampDuration: Duration): IRate {
    override suspend fun delay(startMillis: Long) {
        TODO()
    }

}

private suspend fun doDelay(delay: Duration) {
    when {
        delay == ZERO -> return
        delay < 100.milliseconds -> {
            val deadline = System.nanoTime() + delay.inWholeNanoseconds
            while (System.nanoTime() < deadline){}
        }
        else -> kotlinx.coroutines.delay(delay)
    }
}

class Rate private constructor(private val delay: Duration) {

    suspend fun delay() {
        when {
            delay == ZERO -> return
            delay < 100.milliseconds -> {
                val deadline = System.nanoTime() + delay.inWholeNanoseconds
                while (System.nanoTime() < deadline){}
            }
            else -> kotlinx.coroutines.delay(this.delay)
        }
    }

    override fun toString(): String {
        return "Rate(tps=${1.seconds.div(delay)})"
    }

    companion object {
        val MAX = Rate(Duration.ZERO)
        fun of(tps: Int): Rate {
            require(tps > 0) { "tps must be positive!" }
            return Rate(1.seconds.div(tps))
        }

        fun of(duration: Duration) = Rate(duration)
    }
}