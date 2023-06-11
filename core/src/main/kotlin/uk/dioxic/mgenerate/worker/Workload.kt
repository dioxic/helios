package uk.dioxic.mgenerate.worker

import uk.dioxic.mgenerate.extensions.measureTimedResult

data class Workload(
    override val name: String,
    val executor: Executor,
    val weight: Int = 1,
    val rate: Rate = Rate.MAX,
    val count: Long = Long.MAX_VALUE,
): Named {
    operator fun invoke(workerId: Int) = measureTimedResult {
        executor.invoke(workerId)
    }
}

