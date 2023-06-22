package uk.dioxic.mgenerate.execute.report

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import uk.dioxic.mgenerate.execute.FrameworkMessage
import uk.dioxic.mgenerate.execute.ProgressMessage
import uk.dioxic.mgenerate.execute.StageCompleteMessage
import uk.dioxic.mgenerate.execute.StageStartMessage
import uk.dioxic.mgenerate.execute.results.SummarizedResultsBatch
import uk.dioxic.mgenerate.execute.results.TimedResult
import kotlin.math.max

fun Flow<FrameworkMessage>.format(formatter: ReportFormatter) = formatter.format(this)

enum class ReportFormat {
    TEXT
}

sealed class ReportFormatter {
    abstract fun format(results: Flow<FrameworkMessage>): Flow<String>

    companion object {
        fun create(format: ReportFormat): ReportFormatter = when (format) {
            ReportFormat.TEXT -> ConsoleReportFormatter
        }
    }
}

internal object ConsoleReportFormatter : ReportFormatter() {
    private const val padding = 3
    private const val printHeaderEvery = 10
    private var count = 0L
    private var lastBatchResultCount = 0
    private var columnLengthPairs: List<Pair<String, Int>> = emptyList()

    private fun format(resultBatch: SummarizedResultsBatch) = buildString {
        val results = resultBatch.toMap()
        val columns = results.flatMap { it.keys }.distinct().sorted()

        // print column headers
        if (resultBatch.results.size > 1 || lastBatchResultCount > 1 || count % printHeaderEvery == 0L) {
            columnLengthPairs = columns.map { column ->
                column to max(
                    results.maxOf { it[column]?.length ?: 0 },
                    column.length
                )
            }
            val lineLength = columnLengthPairs.sumOf { it.second + padding } - padding
            appendLine()
            columnLengthPairs.forEachIndexed { index, (column, length) ->
                val pad = if (index == columnLengthPairs.lastIndex) 0 else padding
                appendPaddedAfter(column, length + pad)
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
        lastBatchResultCount = resultBatch.results.size

        count++
    }

    private fun lineBreak(s: String, length: Int = 100) = buildString {
        val before = (length - s.length).div(2)
        appendLine()
        appendChar(before, "-")
        appendSpace()
        append(s)
        appendSpace()
        appendChar(length - before - s.length, "-")
        appendLine()
    }

    override fun format(results: Flow<FrameworkMessage>) = flow {
        results.collect {
            when (it) {
                is StageStartMessage -> emit(lineBreak("Starting ${it.stage.name}"))
                is ProgressMessage -> {
                    when (it.result) {
                        is TimedResult -> emit("\n${it.result.context.workload.name} completed in ${it.result.duration}\n")
                        is SummarizedResultsBatch -> {
                            emit(format(it.result))
                        }
                    }
                }
                is StageCompleteMessage -> emit(lineBreak("Completed ${it.stage.name}"))
            }
        }
    }

    private fun StringBuilder.appendSpace(length: Int = 1): StringBuilder = append(" ".repeat(length))
    private fun StringBuilder.appendChar(length: Int, s: String = " "): StringBuilder = append(s.repeat(length))

    private fun StringBuilder.appendPaddedBefore(value: String, length: Int, padChar: Char = ' '): StringBuilder =
        appendChar(length - value.length, padChar.toString()).append(value)

    private fun StringBuilder.appendPaddedAfter(value: String, length: Int, padChar: Char = ' '): StringBuilder =
        append(value).appendChar(length - value.length, padChar.toString())

}