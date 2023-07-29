package uk.dioxic.helios.execute.results

import uk.dioxic.helios.execute.model.ExecutionContext
import kotlin.time.Duration

class TimedExecutionResult(
    val value: ExecutionResult,
    val duration: Duration,
    val context: ExecutionContext,
    val elapsedTime: Duration = context.startTime.elapsedNow()
) : FrameworkResult