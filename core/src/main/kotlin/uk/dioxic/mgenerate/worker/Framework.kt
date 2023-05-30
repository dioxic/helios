package uk.dioxic.mgenerate.worker

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import org.apache.logging.log4j.kotlin.logger
import uk.dioxic.mgenerate.utils.nextElementIndex
import uk.dioxic.mgenerate.worker.results.*
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = logger("uk.dioxic.mgenerate.worker.Framework")

suspend fun executeStages(vararg stages: Stage, tick: Duration = 1.seconds): Flow<OutputResult> {
    val outputChannel = Channel<OutputResult>()

    stages.forEach {
        when(it) {
            is MultiExecutionStage -> executeStage(it, outputChannel, tick)
            is SingleStage -> {
                outputChannel.send(it.workload.execute(0))
            }
        }
    }

    println("done")

    return outputChannel.consumeAsFlow()
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun executeStage(
    stage: MultiExecutionStage,
    outputChannel: Channel<OutputResult>,
    tick: Duration = 1.seconds
) = coroutineScope {
    val workChannel = Channel<MultiExecutionWorkload>(100)
    val producerJobs = mutableListOf<Job>()

    with(workChannel) {
        val rateLimitedWorkloads = stage.workloads.filterNot { it.rate == Rate.MAX }
        val rateUnlimitedWorkloads = stage.workloads.filter { it.rate == Rate.MAX }

        if (rateUnlimitedWorkloads.isNotEmpty()) {
            producerJobs.add(launch {
                logger.debug { "Launching ${rateUnlimitedWorkloads.size} rate unlimited workloads" }
                produceWork(rateUnlimitedWorkloads, stage.rate)
            })
        }

        rateLimitedWorkloads.forEach {
            producerJobs.add(launch {
                logger.debug { "Launching rate limited workload for ${it.name}" }
                produceWork(it)
            })
        }
    }

    val resultChannel = resultSummarizerActor()

    val deferred = List(stage.workers) {
        launchProcessor(it, workChannel, resultChannel)
    }

    // launch a cancellation coroutine if a timeout is specified
    stage.timeout?.also {
        launch {
            logger.debug { "Launching timeout [$it] coroutine" }
            delay(it)
            logger.debug("timeout reached - cancelling producer jobs")
            producerJobs.forEach { it.cancel() }
        }
    }

    // close work channel when producer jobs are finished
    launch {
        logger.debug("waiting for producer jobs to finish")
        producerJobs.joinAll()
        logger.debug("closing work channel")
        workChannel.close()
    }

    // close results channel when processor jobs are finished
    launch {
        logger.debug("waiting for processor jobs to finish")
        deferred.joinAll()
        logger.debug("closing results channel")
        resultChannel.close()
    }

    launch {
        while (isActive && !resultChannel.isClosedForSend) {
            delay(tick)
            val response = CompletableDeferred<List<SummarizedResult>>()

            resultChannel.trySend(GetSummarizedResults(response)).onSuccess {
                response.await().forEach {
                    outputChannel.send(it)
                }
            }
        }
    }

    println("hello")
}

context(Channel<MultiExecutionWorkload>, CoroutineScope)
private suspend fun produceWork(workload: MultiExecutionWorkload) {
    var count = workload.count
    while (isActive && count > 0) {
        workload.rate.delay()
        send(workload)
        count--
    }
}

context(Channel<MultiExecutionWorkload>, CoroutineScope)
private suspend fun produceWork(workloads: List<MultiExecutionWorkload>, rate: Rate) {
    require(workloads.isNotEmpty())
    val weights = workloads.map(MultiExecutionWorkload::weight).toMutableList()
    val counts = workloads.map(MultiExecutionWorkload::count).toMutableList()
    var weightSum = weights.sum()

    while (isActive && (weights.sum() > 0)) {
        val selection = Random.nextElementIndex(weights, weightSum).let {
            val count = counts[it]--
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

@OptIn(ObsoleteCoroutinesApi::class)
private fun CoroutineScope.resultSummarizerActor() = actor<SummaryResultMessage>(capacity = 100) {
    logger.debug("Starting result summarizer actor")
    val resultsMap = mutableMapOf<String, MutableList<TimedWorkloadResult>>()

    for (msg in channel) {
        when (msg) {
            is TimedWorkloadResult -> {
                resultsMap.getOrPut(msg.workloadName) { mutableListOf() }
                    .add(msg)
            }

            is GetSummarizedResults -> {
                msg.response.complete(resultsMap.map { (k, v) ->
                    v.summarize(k)
                })
                resultsMap.clear()
            }
        }
    }
}

private fun CoroutineScope.launchProcessor(
    id: Int,
    workChannel: ReceiveChannel<MultiExecutionWorkload>,
    resultChannel: SendChannel<SummaryResultMessage>
) = launch(Dispatchers.IO) {
    logger.debug { "Launching work processor $id" }
    for (work in workChannel) {
        resultChannel.send(work.execute(id))
    }
}