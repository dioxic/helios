package uk.dioxic.mgenerate.worker

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.apache.logging.log4j.kotlin.logger
import uk.dioxic.mgenerate.extensions.nextElementIndex
import uk.dioxic.mgenerate.worker.results.*
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private val logger = logger("uk.dioxic.mgenerate.worker.Framework")

fun CoroutineScope.executeStages(vararg stages: Stage, tick: Duration = 1.seconds) = flow {
    stages.forEach {
        when (it) {
            is MultiExecutionStage -> emitAll(executeStage(it, tick))
            is SingleExecutionStage -> {
                emit(it.workload.invoke(0))
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun CoroutineScope.executeStage(
    stage: MultiExecutionStage,
    tick: Duration = 1.seconds
): Flow<SummarizedResultsBatch> {
    val workChannel = Channel<MultiExecutionWorkload>(100)
    val producerJobs = mutableListOf<Job>()

    // launch producers
    with(workChannel) {
        val rateLimitedWorkloads = stage.workloads.filter { it.rate != Rate.MAX && it.count > 0 }
        val rateUnlimitedWorkloads = stage.workloads.filter { it.rate == Rate.MAX && it.count > 0 }

        if (rateUnlimitedWorkloads.isNotEmpty()) {
            producerJobs.add(launch(Dispatchers.Default) {
                logger.trace { "Launching ${rateUnlimitedWorkloads.size} rate unlimited workloads" }
                produceWork(rateUnlimitedWorkloads, stage.rate)
            })
        }

        rateLimitedWorkloads.forEach {
            producerJobs.add(launch(Dispatchers.Default) {
                logger.trace { "Launching rate limited workload for ${it.name}" }
                produceWork(it)
            })
        }
    }

    val resultChannel = resultSummarizerActor()

    // launch processors
    val deferred = List(stage.workers) {
        launchProcessor(it, workChannel, resultChannel)
    }

    // launch a cancellation coroutine if a timeout is specified
    val timeoutJob = stage.timeout?.let {
        launch(Dispatchers.Default) {
            logger.trace { "Launching timeout [$it] coroutine" }
            delay(it)
            logger.debug("timeout of $it reached - cancelling producer jobs")
            producerJobs.forEach { it.cancel() }
        }
    }

    // close work channel when producer jobs are finished
    launch(Dispatchers.Default) {
        logger.trace("waiting for producer jobs to finish")
        producerJobs.joinAll()
        logger.trace("closing work channel")
        workChannel.close()
        logger.trace("cancelling timeout job")
        timeoutJob?.cancel()
    }

    // close results channel when processor jobs are finished
    launch(Dispatchers.Default) {
        logger.trace("waiting for processor jobs to finish")
        deferred.joinAll()
        logger.trace("closing results channel")
        resultChannel.send(CloseAfterNextSummarization)
    }

    // summarize results and return on a flow
    return flow {
        while (!resultChannel.isClosedForSend) {
            delay(tick)
            val response = CompletableDeferred<SummarizedResultsBatch>()
            resultChannel.trySend(GetSummarizedResults(response)).onSuccess {
                emit(response.await())
            }
        }
    }.flowOn(Dispatchers.Default)
}

context(Channel<MultiExecutionWorkload>, CoroutineScope)
private suspend fun produceWork(workload: MultiExecutionWorkload) {
    var count = workload.count
    while (isActive && count > 0) {
        send(workload)
        count--
        workload.rate.delay()
    }
}

context(Channel<MultiExecutionWorkload>, CoroutineScope)
private suspend fun produceWork(workloads: List<MultiExecutionWorkload>, rate: Rate) {
    require(workloads.isNotEmpty())
    val weights = workloads.map { it.weight }.toMutableList()
    val counts = workloads.map { it.count }.toMutableList()
    var weightSum = weights.sum()

    while (isActive && (weightSum > 0)) {
        val selection = Random.nextElementIndex(weights, weightSum).let {
            val count = --counts[it]
            if (count == 0L) {
                weights[it] = 0
                weightSum = weights.sum()
            }
            workloads[it]
        }

        send(selection)
        rate.delay()
    }
}

@OptIn(ObsoleteCoroutinesApi::class, ExperimentalTime::class)
private fun CoroutineScope.resultSummarizerActor() = actor<SummarizationMessage>(capacity = 100) {
    logger.trace("Starting result summarizer actor")
    val resultsMap = mutableMapOf<String, MutableList<TimedResult>>()
    var lastSummaryTime: TimeMark = TimeSource.Monotonic.markNow()
    var scheduleToClose = false

    for (msg in channel) {
        when (msg) {
            is TimedResult -> {
                resultsMap.getOrPut(msg.workloadName) { mutableListOf() }
                    .add(msg)
            }

            is GetSummarizedResults -> {
                val summarizationBatch = SummarizedResultsBatch(
                    duration = lastSummaryTime.elapsedNow(),
                    results = resultsMap.map { (k, v) -> v.summarize(k) }
                        .sortedBy { it.workloadName }
                )
                lastSummaryTime = TimeSource.Monotonic.markNow()
                msg.response.complete(summarizationBatch)
                resultsMap.clear()
                if (scheduleToClose) {
                    channel.close()
                }
            }

            is CloseAfterNextSummarization -> scheduleToClose = true
        }
    }
}

private fun CoroutineScope.launchProcessor(
    id: Int,
    workChannel: ReceiveChannel<MultiExecutionWorkload>,
    resultChannel: SendChannel<SummarizationMessage>
) = launch(Dispatchers.IO) {
    logger.trace { "Launching work processor $id" }
    for (work in workChannel) {
        resultChannel.send(work.invoke(id))
    }
}