package uk.dioxic.mgenerate.execute.report

import uk.dioxic.mgenerate.execute.results.*
import kotlin.time.Duration
import kotlin.time.DurationUnit

enum class ColumnHeader(val display: String) {
    WORKLOAD("workload"),
    INSERT("inserted/s"),
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
    LATENCY_P99("latency P99"),
    LATENCY_MAX("latency max"),
    LATENCY_MIN("latency min"),
    PROGRESS("progress");

    val length: Int
        get() = display.length
}

fun SummarizedResult.toReportColumns(duration: Duration): Map<ColumnHeader, String> = with(duration) {
    contextColumn() + percentileColumns() + scalarColumns()
}

private fun SummarizedResult.contextColumn() = mapOf(
    ColumnHeader.WORKLOAD to context.workload.name,
    ColumnHeader.PROGRESS to context.executionCount.percentOf(context.workload.count)
)

private fun SummarizedResult.percentileColumns() = mapOf(
    ColumnHeader.LATENCY_P50 to latencies.p50.toStringMillis(),
    ColumnHeader.LATENCY_P95 to latencies.p95.toStringMillis(),
    ColumnHeader.LATENCY_P99 to latencies.p99.toStringMillis(),
    ColumnHeader.LATENCY_MAX to latencies.max.toStringMillis(),
    ColumnHeader.LATENCY_MIN to latencies.min.toStringMillis(),
)

context(Duration)
private fun SummarizedResult.scalarColumns() = when (this) {
    is SummarizedWriteResult -> scalarColumns()
    is SummarizedCommandResult -> scalarColumns()
    is SummarizedMessageResult -> scalarColumns()
    is SummarizedReadResult -> scalarColumns()
}.mapValues { (_, v) ->
    v.toString()
}

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

infix fun Int.percentOf(divisor: Int) = "${(this * 100) / divisor}%"

infix fun Long.percentOf(divisor: Long) = "${(this * 100) / divisor}%"

fun Duration.toStringMillis() =
    toString(DurationUnit.MILLISECONDS, 1)