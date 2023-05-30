package uk.dioxic.mgenerate.worker.results

import kotlin.time.Duration

sealed interface TimedWorkloadResult : SummaryResultMessage, OutputResult {
    val value: WorkloadResult
    val duration: Duration
    override val workloadName: String
}

data class TimedWriteResult(
    override val value: WriteResult,
    override val duration: Duration,
    override val workloadName: String
): TimedWorkloadResult

data class TimedReadResult(
    override val value: ReadResult,
    override val duration: Duration,
    override val workloadName: String
): TimedWorkloadResult

data class TimedMessageResult(
    override val value: MessageResult,
    override val duration: Duration,
    override val workloadName: String
): TimedWorkloadResult