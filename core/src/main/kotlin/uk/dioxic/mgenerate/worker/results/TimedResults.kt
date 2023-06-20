package uk.dioxic.mgenerate.worker.results

import uk.dioxic.mgenerate.worker.model.Workload
import kotlin.time.Duration

sealed interface TimedResult : SummarizationMessage, OutputResult {
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