package uk.dioxic.mgenerate.execute.model

import uk.dioxic.mgenerate.execute.measureTimedResult
import uk.dioxic.mgenerate.execute.resources.ResourceRegistry
import uk.dioxic.mgenerate.execute.results.TimedResult
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
        executor.execute()
    }
}
