package uk.dioxic.mgenerate.worker.report

import uk.dioxic.mgenerate.worker.results.*
import kotlin.time.Duration
import kotlin.time.DurationUnit

enum class ColumnHeader(val display: String) {
    WORKLOAD("workload"),
    INSERT("inserts/s"),
    MATCHED("matched/s"),
    MODIFIED("modified/s"),
    DELETED("deleted/s"),
    UPSERTED("upserted/s"),
    DOC_RETURNED("docsReturned/s"),
    QUERIES("queries/s"),
    MSG_COUNT("msgCount/s"),
    SUCCESSES("successes/s"),
    FAILURES("failures/s"),
    LATENCY_P50("latency P50"),
    LATENCY_P95("latency P95"),
    LATENCY_P99("latency P99");

    val length: Int
        get() = display.length
}

fun SummarizedResult.toReportColumns(duration: Duration): Map<ColumnHeader, String> = with(duration) {
    workloadColumn() + percentileColumns() + when (this@toReportColumns) {
        is SummarizedWriteResult -> scalarColumns()
        is SummarizedCommandResult -> scalarColumns()
        is SummarizedMessageResult -> scalarColumns()
        is SummarizedReadResult -> scalarColumns()
    }.mapValues { (_, v) ->
        v.toString()
    }
}

private fun SummarizedResult.workloadColumn() = mapOf(
    ColumnHeader.WORKLOAD to workloadName,
)

private fun SummarizedResult.percentileColumns() =
    latencyPercentiles
        .associate { (name, duration) ->
            ColumnHeader.valueOf("LATENCY_${name.uppercase()}") to duration.toString()
        }
//        .mapKeys { (k, _) -> ColumnHeader.valueOf(k.uppercase()) }

context(Duration)
private fun SummarizedWriteResult.scalarColumns() = mapOf(
    ColumnHeader.INSERT to insertCount.toRate(),
    ColumnHeader.MATCHED to matchedCount.toRate(),
    ColumnHeader.MODIFIED to modifiedCount.toRate(),
    ColumnHeader.DELETED to deletedCount.toRate(),
    ColumnHeader.UPSERTED to upsertedCount.toRate(),
)

context(Duration)
private fun SummarizedReadResult.scalarColumns() = mapOf(
    ColumnHeader.DOC_RETURNED to docReturned.toRate(),
    ColumnHeader.QUERIES to queryCount.toRate(),
)

context(Duration)
private fun SummarizedCommandResult.scalarColumns() = mapOf(
    ColumnHeader.SUCCESSES to successes.toRate(),
    ColumnHeader.FAILURES to failures.toRate(),
)

context(Duration)
private fun SummarizedMessageResult.scalarColumns() = mapOf(
    ColumnHeader.MSG_COUNT to msgCount.toRate()
)

context(Duration)
private fun Long.toRate(): Int =
    (this / toDouble(DurationUnit.SECONDS)).toInt()

context(Duration)
private fun Int.toRate(): Int =
    (this / toDouble(DurationUnit.SECONDS)).toInt()