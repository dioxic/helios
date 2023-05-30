package uk.dioxic.mgenerate.worker

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class Rate private constructor(val delay: Duration) {

    suspend fun delay() {
        if (this.delay != Duration.ZERO) {
            kotlinx.coroutines.delay(this.delay)
        }
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