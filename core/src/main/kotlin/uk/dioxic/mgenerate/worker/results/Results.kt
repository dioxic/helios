package uk.dioxic.mgenerate.worker.results

import kotlinx.coroutines.CompletableDeferred
import uk.dioxic.mgenerate.worker.Workload
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

sealed interface OutputResult {
    val workloadName: String
}

sealed interface SummarizationMessage

class GetSummarizedResults(val response: CompletableDeferred<List<SummarizedResult>>) : SummarizationMessage
object CloseAfterNextSummarization : SummarizationMessage

@OptIn(ExperimentalTime::class)
inline fun Workload.measureTimedResult(block: () -> Result): TimedResult {
    val mark = TimeSource.Monotonic.markNow()
    return when (val value = block()) {
        is WriteResult -> TimedWriteResult(value, mark.elapsedNow(), name)
        is ReadResult -> TimedReadResult(value, mark.elapsedNow(), name)
        is MessageResult -> TimedMessageResult(value, mark.elapsedNow(), name)
    }
}