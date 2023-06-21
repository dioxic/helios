package uk.dioxic.mgenerate.execute.report

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import uk.dioxic.mgenerate.execute.results.OutputResult
import uk.dioxic.mgenerate.execute.results.SummarizedResultsBatch
import uk.dioxic.mgenerate.execute.results.TimedResult
import kotlin.math.max

fun Flow<OutputResult>.format(formatter: ReportFormatter) = formatter.format(this)

enum class ReportFormat {
    TEXT
}

sealed class ReportFormatter {
    abstract fun format(results: Flow<OutputResult>): Flow<String>

    companion object {
        fun create(format: ReportFormat): ReportFormatter = when (format) {
            ReportFormat.TEXT -> ConsoleReportFormatter
        }
    }
}

internal object ConsoleReportFormatter : ReportFormatter() {
    private const val padding = 2
    private const val printHeaderEvery = 10

    private fun formatSummarized(resultBatch: SummarizedResultsBatch, outputCount: Long) = buildString {
        val results = resultBatch.results.map { it.toReportColumns(resultBatch.duration) }
        val columns = results.flatMap { it.keys }.distinct().sorted()
        val columnLengthPairs = columns.map { column ->
            column to max(
                results.maxOf { it[column]?.length ?: 0 },
                column.length
            )
        }
        val lineLength = columnLengthPairs.sumOf { it.second + padding } - padding

        // print column headers
        if (resultBatch.results.size > 1 || outputCount % printHeaderEvery == 0L) {
            appendLine()
            columnLengthPairs.forEachIndexed { index, (column, length) ->
                val pad = if (index == columnLengthPairs.lastIndex) 0 else padding
                appendPaddedAfter(column.display, length + pad)
            }
            appendLine()
            append("".padEnd(lineLength, '-'))
            appendLine()
        }

        // print results
        results.forEach { result ->
            columnLengthPairs.forEachIndexed { index, (column, length) ->
                val pad = if (index == columnLengthPairs.lastIndex) 0 else padding
                appendPaddedAfter(result[column] ?: "0", length + pad)
            }
            appendLine()
        }

        // print totals

    }

    override fun format(results: Flow<OutputResult>) = flow {
        var count = 0L
        results.collect {
            when (it) {
                is SummarizedResultsBatch -> {
                    emit(formatSummarized(it, count))
                    count++
                }

                is TimedResult -> {
                    emit("\n${it.context} completed in ${it.duration}\n")
                }
            }
        }
    }

    private fun StringBuilder.appendSpace(l: Int): StringBuilder = append(" ".repeat(l))

    private fun StringBuilder.appendPaddedBefore(value: String, l: Int): StringBuilder =
        appendSpace(l - value.length).append(value)

    private fun StringBuilder.appendPaddedAfter(value: String, l: Int): StringBuilder =
        append(value).appendSpace(l - value.length)

}