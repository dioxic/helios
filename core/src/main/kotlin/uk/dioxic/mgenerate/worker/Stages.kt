package uk.dioxic.mgenerate.worker

import kotlin.time.Duration

sealed interface Stage {
    val name: String
}

data class MultiExecutionStage(
    override val name: String,
    val workloads: List<MultiExecutionWorkload>,
    val timeout: Duration? = null,
    val rate: Rate = Rate.MAX,
    val workers: Int = 4,
) : Stage

data class SingleStage(
    override val name: String,
    val workload: SingleExecutionWorkload
) : Stage