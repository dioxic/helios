package uk.dioxic.mgenerate.worker.model

import uk.dioxic.mgenerate.worker.results.TimedResult
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

data class WorkloadProgressMessage(
    val workload: Workload,
    val message: TimedResult
) : FrameworkMessage