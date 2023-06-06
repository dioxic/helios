package uk.dioxic.mgenerate.worker.results

import kotlinx.coroutines.CompletableDeferred

sealed interface OutputResult {
    val workloadName: String
}

sealed interface SummarizationMessage

class GetSummarizedResults(val response: CompletableDeferred<List<SummarizedResult>>) : SummarizationMessage
object CloseAfterNextSummarization : SummarizationMessage

