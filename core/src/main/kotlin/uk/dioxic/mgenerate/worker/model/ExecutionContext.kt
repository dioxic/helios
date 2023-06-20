package uk.dioxic.mgenerate.worker.model

import uk.dioxic.mgenerate.extensions.measureTimedResult
import uk.dioxic.mgenerate.resources.ResourceRegistry
import uk.dioxic.mgenerate.worker.results.TimedResult

data class ExecutionContext(
    val workload: Workload,
    val executor: Executor,
    val state: State,
    val rate: Rate,
    val executionCount: Long = 0,
    val startTimeMillis: Long = System.currentTimeMillis()
) {
    fun invoke(resourceRegistry: ResourceRegistry): TimedResult = measureTimedResult(workload) {
        executor.execute(this, resourceRegistry)
    }
}
