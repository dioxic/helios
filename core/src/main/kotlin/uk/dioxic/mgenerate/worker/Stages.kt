package uk.dioxic.mgenerate.worker

import kotlin.time.Duration

sealed interface Stage: Named {
    override val name: String
}

data class MultiExecutionStage(
    override val name: String,
    val workloads: List<MultiExecutionWorkload>,
    val timeout: Duration? = null,
    val rate: Rate = Rate.MAX,
    val workers: Int = 4,
) : Stage

data class SingleExecutionStage(
    override val name: String,
    val executor: Executor
) : Stage {
    operator fun invoke(workerId: Int) = measureTimedResult {
        executor.invoke(workerId)
    }
}