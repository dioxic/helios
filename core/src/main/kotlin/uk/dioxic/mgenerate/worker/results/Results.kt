package uk.dioxic.mgenerate.worker.results

import kotlinx.coroutines.CompletableDeferred
import kotlin.time.Duration

sealed interface OutputResult

data class SummarizedResultsBatch(
    val duration: Duration,
    val results: List<SummarizedResult>
): OutputResult

sealed interface SummarizationMessage

class GetSummarizedResults(val response: CompletableDeferred<SummarizedResultsBatch>) : SummarizationMessage
object CloseAfterNextSummarization : SummarizationMessage

