package uk.dioxic.helios.execute.format

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.bson.Bson
import kotlinx.serialization.bson.encodeToBsonDocument
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.bson.*
import uk.dioxic.helios.execute.*
import uk.dioxic.helios.execute.model.Workload
import uk.dioxic.helios.execute.results.*
import uk.dioxic.helios.execute.serialization.DurationConsoleSerializer
import uk.dioxic.helios.execute.serialization.IntPercentSerializer
import uk.dioxic.helios.generate.flatten
import java.time.Instant
import kotlin.math.max
import kotlin.time.Duration

private typealias Columns = List<Pair<String, Int>>
private typealias ResultMap = Map<String, String>

internal data object ConsoleReportFormatter : ReportFormatter() {
    private const val PADDING = 3
    private const val HEADER_FREQ = 10
    private const val ALWAYS_PRINT_CMD_DOC = false

    private val defaultOutputMap = OutputResult(
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
    ).toFlatMap(Bson { encodeDefaults = true })
    private val fieldOrder = defaultOutputMap
        .map { (k, _) -> k }
        .mapIndexed { i, s -> s to i }
        .toMap()

    private fun formatHeader(columns: Columns) = buildString {
        val lineLength = columns.sumOf { it.second + PADDING } - PADDING
        appendLine()
        columns.forEachIndexed { index, (column, length) ->
            val pad = if (index == columns.lastIndex) 0 else PADDING
            appendPaddedAfter(column, length + pad)
        }
        appendLine()
        append("".padEnd(lineLength, '-'))
    }

    private fun getDefaultValue(key: String) =
        defaultOutputMap[key]?.stringify() ?: error("Default value not found for $key")

    private fun Map<String, String>.getOrDefault(key: String) =
        this.getOrDefault(key, getDefaultValue(key))

    private fun formatResultMap(result: Map<String, String>, columns: Columns) =
        buildString {
            columns.forEachIndexed { index, (column, length) ->
                val pad = if (index == columns.lastIndex) 0 else PADDING
                appendPaddedAfter(result.getOrDefault(column), length + pad)
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

    private fun formatSingleExecution(timedResult: TimedResult) =
        when (timedResult) {
            is TimedExecutionResult -> {
                val outMsg = "\n${timedResult.context.workload.name} completed in ${timedResult.duration}"
                when (timedResult.value) {
                    is MessageResult -> "$outMsg [doc: ${timedResult.value.doc}]"
                    is CommandResult -> {
                        when {
                            ALWAYS_PRINT_CMD_DOC && timedResult.value.document != null ->
                                "$outMsg [res:${timedResult.value.document}]"
                            else ->
                                outMsg
                        }
                    }
                    else -> outMsg
                }
            }
            is TimedExceptionResult -> {
                "\n${timedResult.context.workload.name} failed in ${timedResult.duration} [err: ${timedResult.value.toOutputString()}]"
            }
        }

    override fun format(results: Flow<FrameworkMessage>) = flow {
        var recentWorkloads = mutableListOf<Workload>()
        val recentResults = ArrayDeque<ResultMap>()
        var columns: Columns = emptyList()
        var count: Long = 0

        results.collect { msg ->
            when (msg) {
                is StageStartMessage -> emit(lineBreak("Starting ${msg.stage.name} stage"))
                is ProgressMessage -> {
                    when (val fRes = msg.result) {
                        is TimedResult -> {
                            if (fRes.isSingleExecution) {
                                emit(formatSingleExecution(fRes))
                            } else {
                                val resultsMap = fRes.toResultMap()
                                val headerTick = count % HEADER_FREQ == 0L

                                recentResults.add(resultsMap)

                                if (headerTick || columnChange(columns, resultsMap)) {
                                    columns = getColumns(recentResults)
                                    count = 0
                                    emit(formatHeader(columns))
                                }

                                emit(formatResultMap(resultsMap, columns))

                                if (recentResults.size > HEADER_FREQ) {
                                    recentResults.removeFirst()
                                }
                                count++
                            }
                        }

                        is SummarizedResultsBatch -> {
                            val resultsMap = fRes.toResultMapList()
                            val workloads = fRes.results.map { it.context.workload }
                            val headerTick = count % HEADER_FREQ == 0L
                            val workloadChange =
                                !recentWorkloads.containsAll(workloads) || workloads.size != recentWorkloads.size

                            if (headerTick || workloadChange || workloads.size > 1) {
                                columns = getColumns(resultsMap)
                                count = 0
                                emit(formatHeader(columns))
                            }

                            resultsMap.forEach { result ->
                                emit(formatResultMap(result, columns))
                            }
                            recentWorkloads = workloads.toMutableList()
                            count++
                        }
                    }
                }

                is StageCompleteMessage -> emit(lineBreak("Completed ${msg.stage.name} stage in ${msg.duration.toFormatString()}"))
            }
        }
    }

    private fun columnChange(current: Columns, results: ResultMap): Boolean {
        val new = getColumns(results)
        if (new.size > current.size) {
            return true
        }
        val currentMap = current.toMap()
        new.forEach { (name, len) ->
            val c = currentMap[name]
            if (c == null || c < len) {
                return true
            }
        }

        return false
    }

    private fun getColumns(results: ResultMap): Columns =
        results.keys
            .sortedBy { fieldOrder[it] }
            .map { it to it.length }

    private fun getColumns(results: List<ResultMap>): Columns =
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

    private val json = Bson {
        encodeDefaults = false
        serializersModule = SerializersModule {
            contextual(DurationConsoleSerializer)
            contextual(IntPercentSerializer)
        }
    }

    private fun OutputResult.toStringMap(): Map<String, String> =
        toFlatMap(json)
            .mapValues { (_, v) -> v.stringify() }

    private fun OutputResult.toFlatMap(bson: Bson): Map<String, BsonValue> =
        bson.encodeToBsonDocument(this).flatten(' ', true)

    private fun TimedResult.toResultMap(stageName: String = ""): ResultMap =
        toOutputResult(stageName)
            .toFlatMap(json)
            .mapValues { (_, v) -> v.stringify() }

    private fun SummarizedResult.toResultMap(stageName: String = ""): ResultMap =
        toOutputResult(stageName)
            .toFlatMap(json)
            .mapValues { (_, v) -> v.stringify() }

    private fun SummarizedResultsBatch.toResultMapList(stageName: String = ""): List<ResultMap> =
        results.map { it.toResultMap(stageName) }

    private fun BsonValue.stringify() =
        when (this) {
            is BsonNumber -> this.toNumber().toString()
            is BsonString -> this.value
            is BsonBoolean -> this.value.toString()
            is BsonDateTime -> Instant.ofEpochMilli(this.value).toString()
            else -> error("cannot convert type ${this::class.simpleName} to a String")
        }

    private fun BsonNumber.toNumber(): Number = when (this) {
        is BsonInt32 -> this.value
        is BsonInt64 -> this.value
        is BsonDouble -> this.value
        is BsonDecimal128 -> this.value.bigDecimalValue()
        else -> error("cannot convert type ${this::class.simpleName} to a Number")
    }

}
