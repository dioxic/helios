package uk.dioxic.helios.execute.model

import com.mongodb.MongoException
import uk.dioxic.helios.execute.measureTimedResult
import uk.dioxic.helios.execute.resources.ResourceRegistry
import uk.dioxic.helios.execute.results.ErrorResult
import uk.dioxic.helios.execute.results.TimedResult
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource
import kotlin.time.TimeSource.Monotonic.ValueTimeMark

@OptIn(ExperimentalTime::class)
data class ExecutionContext(
    val workload: Workload,
    val executor: Executor,
    val state: State,
    val rate: Rate,
    val executionCount: Long = 0,
    val startTime: ValueTimeMark = TimeSource.Monotonic.markNow()
) {
    context(ResourceRegistry)
    suspend operator fun invoke(): TimedResult = measureTimedResult {
        try {
            executor.execute()
        } catch (e: MongoException) {
            ErrorResult(e)
        }
    }
}