package uk.dioxic.helios.execute.model

import arrow.optics.optics
import uk.dioxic.helios.execute.measureTimedResult
import uk.dioxic.helios.execute.resources.ResourceRegistry
import uk.dioxic.helios.generate.StateContext
import kotlin.time.TimeSource
import kotlin.time.TimeSource.Monotonic.ValueTimeMark

@optics
data class ExecutionContext(
    val workload: Workload,
    val executor: Executor = workload.executor,
    val rate: Rate,
    val stateContext: List<StateContext>,
    val count: Long,
    val startTime: ValueTimeMark = TimeSource.Monotonic.markNow(),
) {

    context(ResourceRegistry)
    suspend operator fun invoke() = measureTimedResult {
        executor.execute()
    }

    companion object {
        fun create(workloadContext: WorkloadContext, stateContexts: List<StateContext>) =
            ExecutionContext(
                workload = workloadContext.workload,
                executor = workloadContext.executor,
                rate = workloadContext.rate,
                count = workloadContext.executionId,
                stateContext = stateContexts,
            )
    }
}
