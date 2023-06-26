@file:OptIn(ExperimentalTime::class)

package uk.dioxic.mgenerate.execute.results

import uk.dioxic.mgenerate.execute.model.ExecutionContext
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

sealed interface TimedResult: FrameworkResult {
    val value: ExecutionResult
    val duration: Duration
    val context: ExecutionContext
    val elapsedTime: Duration
}

data class TimedCommandResult(
    override val value: CommandResult,
    override val duration: Duration,
    override val context: ExecutionContext,
    override val elapsedTime: Duration = context.startTime.elapsedNow()
): TimedResult

data class TimedWriteResult(
    override val value: WriteResult,
    override val duration: Duration,
    override val context: ExecutionContext,
    override val elapsedTime: Duration = context.startTime.elapsedNow(),
): TimedResult

data class TimedReadResult(
    override val value: ReadResult,
    override val duration: Duration,
    override val context: ExecutionContext,
    override val elapsedTime: Duration = context.startTime.elapsedNow(),
): TimedResult

data class TimedTransactionResult(
    override val value: TransactionResult,
    override val duration: Duration,
    override val context: ExecutionContext,
    override val elapsedTime: Duration = context.startTime.elapsedNow(),
): TimedResult

data class TimedMessageResult(
    override val value: MessageResult,
    override val duration: Duration,
    override val context: ExecutionContext,
    override val elapsedTime: Duration = context.startTime.elapsedNow(),
): TimedResult

data class TimedErrorResult(
    override val value: ErrorResult,
    override val duration: Duration,
    override val context: ExecutionContext,
    override val elapsedTime: Duration = context.startTime.elapsedNow(),
): TimedResult