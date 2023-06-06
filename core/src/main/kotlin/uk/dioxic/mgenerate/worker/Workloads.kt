package uk.dioxic.mgenerate.worker

import uk.dioxic.mgenerate.worker.results.measureTimedResult

sealed interface Workload {
    val name: String
    val executor: Executor
    operator fun invoke(workerId: Int) = measureTimedResult {
        executor.invoke(workerId)
    }
}

data class SingleExecutionWorkload(
    override val name: String,
    override val executor: Executor
) : Workload

data class MultiExecutionWorkload(
    override val name: String,
    override val executor: Executor,
    val weight: Int = 1,
    val rate: Rate = Rate.MAX,
    val count: Long = Long.MAX_VALUE,
) : Workload
