package uk.dioxic.helios.execute.model

import arrow.optics.optics
import com.mongodb.MongoException
import uk.dioxic.helios.execute.measureTimedResult
import uk.dioxic.helios.execute.resources.ResourceRegistry
import uk.dioxic.helios.execute.results.ErrorResult
import uk.dioxic.helios.execute.results.TimedResult
import uk.dioxic.helios.generate.StateContext
import kotlin.time.TimeSource
import kotlin.time.TimeSource.Monotonic.ValueTimeMark

@optics
data class ExecutionContext(
    val workload: Workload,
    val executor: Executor = workload.executor,
    val rate: Rate,
    val stateContext: List<StateContext>,
    val count: Long = 0,
    val startTime: ValueTimeMark = TimeSource.Monotonic.markNow(),
)  {

    context(ResourceRegistry)
    suspend operator fun invoke(): TimedResult = measureTimedResult {
        try {
            executor.execute()
        } catch (e: MongoException) {
            ErrorResult(e)
        }
    }

    companion object {
        fun create(workloadContext: WorkloadContext, stateContexts: List<StateContext>) =
            ExecutionContext(
                workload = workloadContext.workload,
                executor = workloadContext.executor,
                rate = workloadContext.rate,
                stateContext = stateContexts,
            )
    }
}
