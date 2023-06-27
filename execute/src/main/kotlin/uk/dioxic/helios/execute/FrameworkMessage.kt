package uk.dioxic.helios.execute

import uk.dioxic.helios.execute.model.Stage
import uk.dioxic.helios.execute.results.FrameworkResult
import kotlin.time.Duration

sealed interface FrameworkMessage

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