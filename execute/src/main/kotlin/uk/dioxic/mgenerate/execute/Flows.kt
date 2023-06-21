package uk.dioxic.mgenerate.execute

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.selects.whileSelect
import uk.dioxic.mgenerate.execute.results.OutputResult
import uk.dioxic.mgenerate.execute.results.SummarizedResultsBatch
import uk.dioxic.mgenerate.execute.results.TimedResult
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.TimeSource

suspend fun Flow<Number>.average(): Double {
    var sum = 0.0
    var count = 0L
    collect {
        ++count
        sum += it.toDouble()
        if (count < 0L) {
            throw ArithmeticException("Count overflow has happened.")
        }
    }

    return if (count == 0L) Double.NaN else sum / count
}

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class, ObsoleteCoroutinesApi::class)
fun Flow<TimedResult>.summarize(interval: Duration): Flow<OutputResult> = channelFlow {
    require(interval.isPositive()) {
        "interval must be positive, but was $interval"
    }
    val results = ArrayList<TimedResult>()
    val flowChannel = produce { collect { send(it) } }
    val tickerChannel = ticker(interval.inWholeMilliseconds)
    var lastSummaryTime: TimeMark = TimeSource.Monotonic.markNow()

    try {
        whileSelect {
            flowChannel.onReceive {
                if (it.isSingleExecution ){
                    send(it)
                    true
                }
                else {
                    results.add(it)
                }
            }
            tickerChannel.onReceive {
                send(SummarizedResultsBatch(
                    duration = lastSummaryTime.elapsedNow(),
                    results = results.summarize()
                ))
                lastSummaryTime = TimeSource.Monotonic.markNow()
                results.clear()
                true
            }
        }
    } catch (e: ClosedReceiveChannelException) {
        if (results.isNotEmpty()) {
            send(SummarizedResultsBatch(
                duration = lastSummaryTime.elapsedNow(),
                results = results.summarize()
            ))
        }
    } finally {
        tickerChannel.cancel()
    }
}

private val TimedResult.isSingleExecution
    get() = (context.workload.count == 1L)

private fun List<TimedResult>.summarize() =
    groupBy(TimedResult::context)
        .map { (k, v) -> v.summarize(k) }
        .sortedBy { it.context.workload.name }
