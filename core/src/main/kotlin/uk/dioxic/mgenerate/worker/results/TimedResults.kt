package uk.dioxic.mgenerate.worker.results

import kotlin.time.Duration

sealed interface TimedResult : SummarizationMessage, OutputResult {
    val value: Result
    val duration: Duration
    override val workloadName: String
}

data class TimedCommandResult(
    override val value: CommandResult,
    override val duration: Duration,
    override val workloadName: String
): TimedResult

data class TimedWriteResult(
    override val value: WriteResult,
    override val duration: Duration,
    override val workloadName: String
): TimedResult

data class TimedReadResult(
    override val value: ReadResult,
    override val duration: Duration,
    override val workloadName: String
): TimedResult

data class TimedMessageResult(
    override val value: MessageResult,
    override val duration: Duration,
    override val workloadName: String
): TimedResult