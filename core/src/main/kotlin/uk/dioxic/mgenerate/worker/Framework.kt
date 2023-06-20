package uk.dioxic.mgenerate.worker

import arrow.fx.coroutines.parMapUnordered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import uk.dioxic.mgenerate.extensions.nextElementIndex
import uk.dioxic.mgenerate.resources.ResourceRegistry
import uk.dioxic.mgenerate.worker.model.*
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class, FlowPreview::class, ExperimentalCoroutinesApi::class)
fun executeBenchmark(benchmark: Benchmark, registry: ResourceRegistry, workers: Int = 4): Flow<FrameworkMessage> =
    flow {

        benchmark.stages.forEach { stage ->
            emit(StageStartMessage(stage))
            val duration = measureTime {
                when (stage) {
                    is SequentialStage -> {
                        stage.workloads
                            .forEach { workload ->
                                produceRated(benchmark, stage, workload)
                                    .parMapUnordered(workers) { it.invoke(registry) }
                                    .map { WorkloadProgressMessage(workload, it) }
                                    .collect { emit(it) }
                            }
                    }

                    is ParallelStage -> {
                        buildList {
                            // weighted workloads
                            addAll(stage.workloads.filterIsInstance<RateWorkload>()
                                .map { produceRated(benchmark, stage, it) })

                            // rate workloads
                            add(
                                produceWeighted(
                                    benchmark = benchmark,
                                    stage = stage,
                                    workloads = stage.workloads.filterIsInstance<WeightedWorkload>()
                                )
                            )
                        }.merge()
                            .parMapUnordered(workers) { it.invoke(registry) }
                            .map { WorkloadProgressMessage(it.workload, it) }
                            .collect { emit(it) }
                    }
                }
            }
            emit(StageCompleteMessage(stage, duration))
        }
    }

fun produceWeighted(
    benchmark: Benchmark,
    stage: Stage,
    workloads: List<WeightedWorkload>
): Flow<ExecutionContext> = flow {
    val contexts = workloads.map { it.createContext(benchmark, stage) }
    val weights = workloads.map { it.weight }.toMutableList()
    val counts = MutableList(workloads.size) { 0L }

    var weightSum = weights.sum()
    while (weightSum > 0) {
        val context = Random.nextElementIndex(weights, weightSum).let {
            val count = ++counts[it]
            if (count == workloads[it].count) {
                weights[it] = 0
                weightSum = weights.sum()
            }
            contexts[it].copy(executionCount = count)
        }
        emit(context)
        delay(context)
    }
}

fun produceRated(
    benchmark: Benchmark,
    stage: Stage,
    workload: RateWorkload
): Flow<ExecutionContext> = flow {
    val context = workload.createContext(benchmark, stage)
    for (i in 1..context.workload.count) {
        context.copy(executionCount = i).also {
            emit(it)
            delay(it)
        }
    }
}

suspend fun delay(context: ExecutionContext) {
    val delay = context.rate.calculateDelay(context.state)
    when {
        delay == Duration.ZERO -> return
        delay < 100.milliseconds -> {
            val deadline = System.nanoTime() + delay.inWholeNanoseconds
            while (System.nanoTime() < deadline) {
            }
        }

        else -> kotlinx.coroutines.delay(delay)
    }
}