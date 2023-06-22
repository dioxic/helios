package uk.dioxic.mgenerate.execute

import uk.dioxic.mgenerate.execute.model.Stage
import uk.dioxic.mgenerate.execute.model.Workload
import uk.dioxic.mgenerate.execute.results.FrameworkResult
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
    val result: FrameworkResult
) : FrameworkMessage