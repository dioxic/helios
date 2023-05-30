package uk.dioxic.mgenerate.worker.results

import kotlinx.coroutines.CompletableDeferred
import uk.dioxic.mgenerate.worker.Workload
import kotlin.time.*

sealed interface ResultMessage

class GetSummarizedResults(val response: CompletableDeferred<Map<String, SummarizedResult>>) : ResultMessage

context(Workload)
@OptIn(ExperimentalTime::class)
inline fun measureTimedWorkloadValue(block: () -> WorkloadResult): TimedWorkloadResult {
    val mark = TimeSource.Monotonic.markNow()
    return when (val value = block()) {
        is WriteResult -> TimedWriteResult(value, mark.elapsedNow(), name)
        is ReadResult -> TimedReadResult(value, mark.elapsedNow(), name)
        is MessageResult -> TimedMessageResult(value, mark.elapsedNow(), name)
    }
}