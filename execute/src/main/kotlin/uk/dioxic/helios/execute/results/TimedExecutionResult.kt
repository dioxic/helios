package uk.dioxic.helios.execute.results

import com.mongodb.MongoException
import uk.dioxic.helios.execute.model.ExecutionContext
import kotlin.time.Duration

sealed interface TimedResult : FrameworkResult {
    val duration: Duration
    val context: ExecutionContext
    val elapsedTime: Duration
}

class TimedExecutionResult(
    val value: ExecutionResult,
    override val duration: Duration,
    override val context: ExecutionContext,
    override val elapsedTime: Duration = context.startTime.elapsedNow()
) : TimedResult

class TimedExceptionResult(
    val value: MongoException,
    override val duration: Duration,
    override val context: ExecutionContext,
    override val elapsedTime: Duration = context.startTime.elapsedNow()
) : TimedResult