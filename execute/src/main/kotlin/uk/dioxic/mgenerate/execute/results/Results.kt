package uk.dioxic.mgenerate.execute.results

import kotlin.time.Duration

sealed interface OutputResult

data class SummarizedResultsBatch(
    val duration: Duration,
    val results: List<SummarizedResult>
): OutputResult

