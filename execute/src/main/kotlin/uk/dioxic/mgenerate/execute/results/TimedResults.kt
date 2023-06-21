package uk.dioxic.mgenerate.execute.results

import uk.dioxic.mgenerate.execute.model.ExecutionContext
import kotlin.time.Duration

sealed interface TimedResult: OutputResult {
    val context: ExecutionContext
    val value: Result
    val duration: Duration
}

data class TimedCommandResult(
    override val value: CommandResult,
    override val duration: Duration,
    override val context: ExecutionContext
): TimedResult

data class TimedWriteResult(
    override val value: WriteResult,
    override val duration: Duration,
    override val context: ExecutionContext
): TimedResult

data class TimedReadResult(
    override val value: ReadResult,
    override val duration: Duration,
    override val context: ExecutionContext
): TimedResult

data class TimedMessageResult(
    override val value: MessageResult,
    override val duration: Duration,
    override val context: ExecutionContext
): TimedResult