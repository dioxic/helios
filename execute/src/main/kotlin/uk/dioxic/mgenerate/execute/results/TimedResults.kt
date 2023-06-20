package uk.dioxic.mgenerate.execute.results

import uk.dioxic.mgenerate.execute.model.Workload
import kotlin.time.Duration

sealed interface TimedResult {
    val workload: Workload
    val value: Result
    val duration: Duration
}

data class TimedCommandResult(
    override val value: CommandResult,
    override val duration: Duration,
    override val workload: Workload
): TimedResult

data class TimedWriteResult(
    override val value: WriteResult,
    override val duration: Duration,
    override val workload: Workload
): TimedResult

data class TimedReadResult(
    override val value: ReadResult,
    override val duration: Duration,
    override val workload: Workload
): TimedResult

data class TimedMessageResult(
    override val value: MessageResult,
    override val duration: Duration,
    override val workload: Workload
): TimedResult