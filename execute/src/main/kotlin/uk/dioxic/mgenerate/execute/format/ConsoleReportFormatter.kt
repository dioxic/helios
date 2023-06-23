package uk.dioxic.mgenerate.execute.format

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import uk.dioxic.mgenerate.execute.FrameworkMessage
import uk.dioxic.mgenerate.execute.ProgressMessage
import uk.dioxic.mgenerate.execute.StageCompleteMessage
import uk.dioxic.mgenerate.execute.StageStartMessage
import uk.dioxic.mgenerate.execute.model.Workload
import uk.dioxic.mgenerate.execute.results.SummarizedResultsBatch
import uk.dioxic.mgenerate.execute.results.TimedResult
import kotlin.math.max

typealias ColumnLengths = List<Pair<String, Int>>

internal object ConsoleReportFormatter : ReportFormatter() {
    private const val padding = 3
    private const val printHeaderEvery = 10

    private fun format(resultsMap: ResultsMap, columnLengths: ColumnLengths, printHeader: Boolean) = buildString {
        // print column headers
        if (printHeader) {
            val lineLength = columnLengths.sumOf { it.second + padding } - padding
            appendLine()
            columnLengths.forEachIndexed { index, (column, length) ->
                val pad = if (index == columnLengths.lastIndex) 0 else padding
                appendPaddedAfter(column, length + pad)
            }
            appendLine()
            append("".padEnd(lineLength, '-'))
            appendLine()
        }

        // print results
        resultsMap.forEach { result ->
            columnLengths.forEachIndexed { index, (column, length) ->
                val pad = if (index == columnLengths.lastIndex) 0 else padding
                appendPaddedAfter(result[column] ?: "0", length + pad)
            }
            appendLine()
        }

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
        var lastWorkloads: List<Workload> = emptyList()
        var columnLengths: ColumnLengths = emptyList()
        var count: Long = 0

        results.collect { msg ->
            when (msg) {
                is StageStartMessage -> emit(lineBreak("Starting ${msg.stage.name}"))
                is ProgressMessage -> {
                    when (msg.result) {
                        is TimedResult -> emit("\n${msg.result.context.workload.name} completed in ${msg.result.duration}\n")
                        is SummarizedResultsBatch -> {
                            val resultsMap = msg.result.toMap()
                            val workloads = msg.result.results.map { it.context.workload }
                            val headerTick = count % printHeaderEvery == 0L
                            val workloadChange = !lastWorkloads.containsAll(workloads) || workloads.size != lastWorkloads.size

                            val printHeader = (headerTick || workloadChange || workloads.size > 1).also {
                                if (it) {
                                    columnLengths = getColumnLengths(resultsMap)
                                    count = 0
                                }
                            }

                            emit(format(resultsMap, columnLengths, printHeader))
                            lastWorkloads = workloads
                            count++
                        }
                    }
                }

                is StageCompleteMessage -> emit(lineBreak("Completed ${msg.stage.name}"))
            }
        }
    }

    private fun getColumnLengths(results: ResultsMap): ColumnLengths =
        results
            .flatMap { it.keys }
            .distinct()
            .sorted()
            .map { column ->
                column to max(
                    results.maxOf { it[column]?.length ?: 0 },
                    column.length
                )
            }

    private fun StringBuilder.appendSpace(length: Int = 1): StringBuilder = append(" ".repeat(length))
    private fun StringBuilder.appendChar(length: Int, s: String = " "): StringBuilder = append(s.repeat(length))

    private fun StringBuilder.appendPaddedBefore(value: String, length: Int, padChar: Char = ' '): StringBuilder =
        appendChar(length - value.length, padChar.toString()).append(value)

    private fun StringBuilder.appendPaddedAfter(value: String, length: Int, padChar: Char = ' '): StringBuilder =
        append(value).appendChar(length - value.length, padChar.toString())

}