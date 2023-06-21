package uk.dioxic.mgenerate.execute.model

import uk.dioxic.mgenerate.execute.measureTimedResult
import uk.dioxic.mgenerate.execute.resources.ResourceRegistry
import uk.dioxic.mgenerate.execute.results.TimedResult

data class ExecutionContext(
    val workload: Workload,
    val executor: Executor,
    val state: State,
    val rate: Rate,
    val executionCount: Long = 0,
    val startTimeMillis: Long = System.currentTimeMillis()
) {
    fun invoke(resourceRegistry: ResourceRegistry): TimedResult = measureTimedResult(this) {
        executor.execute(this, resourceRegistry)
    }
}
