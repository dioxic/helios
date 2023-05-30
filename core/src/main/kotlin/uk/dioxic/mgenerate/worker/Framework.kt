package uk.dioxic.mgenerate.worker

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import uk.dioxic.mgenerate.utils.nextElement
import uk.dioxic.mgenerate.worker.results.GetSummarizedResults
import uk.dioxic.mgenerate.worker.results.ResultMessage
import uk.dioxic.mgenerate.worker.results.TimedWorkloadResult
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.time.Duration

fun CoroutineScope.executeWorkloads(
    workloads: List<Workload>,
//    timeout: Duration,
    rate: Rate = Rate.MAX,
    workers: Int = 4,
): SendChannel<ResultMessage> {

    val workChannel = Channel<Workload>(100)
    with(workChannel) {
        val rateLimitedWorkloads = workloads.filterNot { it.rate == Rate.MAX }
        val rateUnlimitedWorkloads = workloads.filter { it.rate == Rate.MAX }

        launch {
            produceWork(rateUnlimitedWorkloads, rate)
        }

        rateLimitedWorkloads.forEach {
            launch {
                produceWork(it)
            }
        }
    }

    val resultChannel = resultAggregatorActor()

    val deferred = List(workers) {
        launchProcessor(it, workChannel, resultChannel)
    }
    launch {
        deferred.joinAll()
        resultChannel.close()
    }

    return resultChannel
}

context(Channel<Workload>, CoroutineScope)
private suspend fun produceWork(workload: Workload) {
    var count = workload.count
    while (isActive && (count == null || count > 0)) {
        workload.rate.delay()
        send(workload)
        if (count != null) {
            count--
        }
    }
}

context(Channel<Workload>, CoroutineScope)
private suspend fun produceWork(workloads: List<Workload>, rate: Rate) {
    require(workloads.isNotEmpty())
    val weights = workloads.map(Workload::weight).toMutableList()
    val counts = workloads
        .associateWith { it.count }.toMutableMap()

    while (isActive && (weights.sum() > 0)) {
        rate.delay()
        val selection = Random.nextElement(workloads, weights)!!
        send(selection)
        counts[selection]?.also {
            when {
                it == 1L -> {
                    weights[workloads.indexOf(selection)] = 0
                    counts[selection] = 0L
                }

                it > 0L -> counts[selection] = it.dec()
            }
        }
    }
    close()
}

@OptIn(ObsoleteCoroutinesApi::class)
private fun CoroutineScope.resultAggregatorActor() = actor<ResultMessage>(capacity = 100) {
    val resultsMap = mutableMapOf<String, MutableList<TimedWorkloadResult>>()

    for (msg in channel) {
        when (msg) {
            is TimedWorkloadResult -> {
                resultsMap.getOrPut(msg.workloadName) { mutableListOf() }
                    .add(msg)
            }

            is GetSummarizedResults -> {
                msg.response.complete(resultsMap.mapValues { (_, v) ->
                    v.summarize()
                })
                resultsMap.clear()
            }
        }
    }
}

private fun CoroutineScope.launchProcessor(
    id: Int,
    workChannel: ReceiveChannel<Workload>,
    resultChannel: SendChannel<ResultMessage>
) = launch(Dispatchers.IO) {
    for (work in workChannel) {
        resultChannel.send(work.execute(id))
    }
}