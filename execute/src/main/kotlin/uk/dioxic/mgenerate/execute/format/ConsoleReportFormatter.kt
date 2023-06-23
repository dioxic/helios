package uk.dioxic.mgenerate.execute.format

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import uk.dioxic.mgenerate.execute.FrameworkMessage
import uk.dioxic.mgenerate.execute.ProgressMessage
import uk.dioxic.mgenerate.execute.StageCompleteMessage
import uk.dioxic.mgenerate.execute.StageStartMessage
import uk.dioxic.mgenerate.execute.model.Workload
import uk.dioxic.mgenerate.execute.results.SummarizedLatencies
import uk.dioxic.mgenerate.execute.results.SummarizedResultsBatch
import uk.dioxic.mgenerate.execute.results.TimedResult
import uk.dioxic.mgenerate.execute.serialization.DurationConsoleSerializer
import uk.dioxic.mgenerate.execute.serialization.IntPercentSerializer
import kotlin.math.max
import kotlin.time.Duration

private typealias Columns = List<Pair<String, Int>>
private typealias ResultsMap = List<Map<String, String>>

internal object ConsoleReportFormatter : ReportFormatter() {
    private const val padding = 3
    private const val printHeaderEvery = 10

    private fun formatHeader(columns: Columns) = buildString {
        val lineLength = columns.sumOf { it.second + padding } - padding
        appendLine()
        columns.forEachIndexed { index, (column, length) ->
            val pad = if (index == columns.lastIndex) 0 else padding
            appendPaddedAfter(column, length + pad)
        }
        appendLine()
        append("".padEnd(lineLength, '-'))
    }

    private fun formatResult(result: Map<String, String>, columns: Columns) =
        buildString {
            columns.forEachIndexed { index, (column, length) ->
                val pad = if (index == columns.lastIndex) 0 else padding
                appendPaddedAfter(result[column] ?: "0", length + pad)
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
    }

    override fun format(results: Flow<FrameworkMessage>) = flow {
        var lastWorkloads: List<Workload> = emptyList()
        var columns: Columns = emptyList()
        var count: Long = 0

        results.collect { msg ->
            when (msg) {
                is StageStartMessage -> emit(lineBreak("Starting ${msg.stage.name}"))
                is ProgressMessage -> {
                    when (val fRes = msg.result) {
                        is TimedResult -> emit("\n${fRes.context.workload.name} completed in ${fRes.duration}")
                        is SummarizedResultsBatch -> {
                            val resultsMap = fRes.toFlatMap()
                            val workloads = fRes.results.map { it.context.workload }
                            val headerTick = count % printHeaderEvery == 0L
                            val workloadChange =
                                !lastWorkloads.containsAll(workloads) || workloads.size != lastWorkloads.size

                            if (headerTick || workloadChange || workloads.size > 1) {
                                columns = getColumns(resultsMap)
                                count = 0
                                emit(formatHeader(columns))
                            }

                            resultsMap.forEach { result ->
                                emit(formatResult(result, columns))
                            }
                            lastWorkloads = workloads
                            count++
                        }
                    }
                }

                is StageCompleteMessage -> emit(lineBreak("Completed ${msg.stage.name} in ${msg.duration.toFormatString()}"))
            }
        }
    }

    private fun getColumns(results: ResultsMap): Columns =
        results
            .flatMap { it.keys }
            .distinct()
            .sortedBy { fieldOrder[it] }
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

    private val json = Json {
        encodeDefaults = false
        serializersModule = SerializersModule {
            contextual(DurationConsoleSerializer)
            contextual(IntPercentSerializer)
        }
    }

    private fun OutputResult.toFlatMap(json: Json): Map<String, JsonElement> =
        json.encodeToJsonElement(this).jsonObject.flatten(' ')

    private fun SummarizedResultsBatch.toFlatMap(stageName: String = ""): ResultsMap = results.map { sr ->
        with(batchDuration) {
            sr.toOutputResult(stageName).toFlatMap(json).mapValues { (_, v) ->
                when (v) {
                    is JsonPrimitive -> v.content
//                    is kotlinx.serialization.json.JsonObject -> {              }
                    else -> error("${v::class} not supported")
                }
            }
        }
    }

    private val fieldOrder = OutputResult(
        workloadName = "",
        operationCount = 0,
        elapsed = Duration.ZERO,
        progress = 100,
        latencies = SummarizedLatencies(
            p50 = Duration.ZERO,
            p95 = Duration.ZERO,
            p99 = Duration.ZERO,
            max = Duration.ZERO
        )
    ).toFlatMap(Json { encodeDefaults = true })
        .map { (k, _) -> k }
        .mapIndexed { i, s -> s to i }
        .toMap()

}
