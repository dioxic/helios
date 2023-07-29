package uk.dioxic.helios.execute.results

import org.apache.commons.math3.stat.StatUtils
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun List<Duration>.summarize() = SummarizedLatencies(
    p50 = percentile(50.0),
    p95 = percentile(95.0),
    p99 = percentile(99.0),
    max = max(),
)

fun Iterable<Duration>.percentile(percentile: Double) =
    map { it.toDouble(DurationUnit.MILLISECONDS) }.percentile(percentile)

fun Sequence<Duration>.percentile(percentile: Double) =
    map { it.toDouble(DurationUnit.MILLISECONDS) }
        .toList().percentile(percentile)

fun List<Double>.percentile(percentile: Double) =
    StatUtils.percentile(toDoubleArray(), percentile)
        .toDuration(DurationUnit.MILLISECONDS)

fun List<TimedExecutionResult>.summarize() =
    groupBy { it.context.workload.name }
        .map { (_, v) ->
            v.fold(ResultAccumulator(), ResultAccumulator::add).toSummarizedResult()
        }.sortedBy { it.context.workload.name }