package uk.dioxic.mgenerate.worker

import uk.dioxic.mgenerate.worker.results.*
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

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

@OptIn(ExperimentalTime::class)
inline fun Workload.measureTimedResult(block: () -> Result): TimedResult {
    val mark = TimeSource.Monotonic.markNow()
    return when (val value = block()) {
        is WriteResult -> TimedWriteResult(value, mark.elapsedNow(), name)
        is ReadResult -> TimedReadResult(value, mark.elapsedNow(), name)
        is MessageResult -> TimedMessageResult(value, mark.elapsedNow(), name)
        is CommandResult -> TimedCommandResult(value, mark.elapsedNow(), name)
    }
}