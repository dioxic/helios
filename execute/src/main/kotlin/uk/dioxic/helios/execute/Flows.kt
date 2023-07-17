@file:Suppress("FunctionName")

package uk.dioxic.helios.execute

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.whileSelect
import uk.dioxic.helios.execute.results.FrameworkResult
import uk.dioxic.helios.execute.results.SummarizedResultsBatch
import uk.dioxic.helios.execute.results.TimedResult
import uk.dioxic.helios.execute.results.summarize
import kotlin.time.Duration
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

@OptIn(ExperimentalCoroutinesApi::class, ObsoleteCoroutinesApi::class)
fun Flow<TimedResult>.chunked(interval: Duration): Flow<FrameworkResult> {
    return if (interval.isPositive()) {
        channelFlow {
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
                        if (it.isSingleExecution) {
                            send(it)
                            true
                        } else {
                            results.add(it)
                        }
                    }
                    tickerChannel.onReceive {
                        if (results.isNotEmpty()) {
                            send(
                                SummarizedResultsBatch(
                                    batchDuration = lastSummaryTime.elapsedNow(),
                                    results = results.summarize()
                                )
                            )
                            lastSummaryTime = TimeSource.Monotonic.markNow()
                            results.clear()
                        }
                        true
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                if (results.isNotEmpty()) {
                    send(
                        SummarizedResultsBatch(
                            batchDuration = lastSummaryTime.elapsedNow(),
                            results = results.summarize()
                        )
                    )
                }
            } finally {
                tickerChannel.cancel()
            }
        }
    } else {
        this
    }
}

private val TimedResult.isSingleExecution
    get() = (context.workload.count == 1L)

fun SharingStarted.Companion.WhileSubscribedAtLeast(threshold: Int) =
    SharingStarted { subscriptionCount ->
        subscriptionCount
            .map { if (it >= threshold) SharingCommand.START else SharingCommand.STOP }
            .dropWhile { it != SharingCommand.START } // don't emit any STOP/RESET_BUFFER to start with, only START
            .distinctUntilChanged() // just in case somebody forgets it, don't leak our multiple sending of START
    }
