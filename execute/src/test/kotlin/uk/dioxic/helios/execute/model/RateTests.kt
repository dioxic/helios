package uk.dioxic.helios.execute.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import uk.dioxic.helios.execute.defaultExecutionContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

@ExperimentalTime
class RateTests : FunSpec({

    test("ramp up rate calculation") {
        val context = defaultExecutionContext.copy(startTime = TimeSource.Monotonic.markNow())
        val rampRate = RampedRate(from = TpsRate(100), to = UnlimitedRate, rampDuration = 2.seconds)

        val rampDelays = (0..5).asFlow().map { _ ->
            val rampDelay = with(context) {
                 rampRate.calculateDelay()
            }
            delay(500.milliseconds)
            rampDelay
        }.toList()

        rampDelays.first() shouldBeGreaterThan 9.milliseconds
        rampDelays.last() shouldBe 0.milliseconds

    }

})