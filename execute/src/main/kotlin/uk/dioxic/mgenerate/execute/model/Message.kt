package uk.dioxic.mgenerate.execute.model

import uk.dioxic.mgenerate.execute.results.OutputResult
import kotlin.time.Duration

sealed interface FrameworkMessage

data class WorkloadStartMessage(
    val workload: Workload
) : FrameworkMessage

data class WorkloadCompleteMessage(
    val workload: Workload
) : FrameworkMessage

data class StageStartMessage(
    val stage: Stage
) : FrameworkMessage

data class StageCompleteMessage(
    val stage: Stage,
    val duration: Duration
) : FrameworkMessage

data class ProgressMessage(
    val stage: Stage,
    val result: OutputResult
) : FrameworkMessage