package uk.dioxic.mgenerate.worker

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class Rate private constructor(val delay: Duration) {

    suspend fun delay() {
        if (this.delay > 100.milliseconds) {
            kotlinx.coroutines.delay(this.delay)
        } else {
            val deadline = System.nanoTime() + delay.inWholeNanoseconds
            while (System.nanoTime() < deadline){}
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

inline val Int.tps get() = Rate.of(this)