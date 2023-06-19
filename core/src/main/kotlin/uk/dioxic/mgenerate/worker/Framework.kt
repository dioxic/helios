package uk.dioxic.mgenerate.worker

import arrow.core.NonEmptyList
import arrow.fx.coroutines.parMap
import com.mongodb.client.MongoClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import uk.dioxic.mgenerate.extensions.nextElementIndex
import uk.dioxic.mgenerate.worker.model.*
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class, FlowPreview::class, ExperimentalCoroutinesApi::class)
fun executeBenchmark(benchmark: Benchmark, client: MongoClient, workers: Int = 4): Flow<FrameworkMessage> = flow {
    val benchmarkState = benchmark.hydratedState

    benchmark.stages.forEach { stage ->
        val stageState = benchmarkState + stage.hydratedState

        when (stage) {
            is ParallelStage -> TODO()
            is SequentialStage -> {
                emit(StageStartMessage(stage))
                val duration = measureTime {
                    stage.workloads.forEach { workload ->
                        produceWork(workload, stageState, client)
//                            .onStart { emit(WorkloadStartMessage(workload)) }
//                            .onCompletion { emit(WorkloadCompleteMessage(workload)) }
                            .parMap(workers) { it.invoke() }
                            .map { WorkloadProgressMessage(workload, it) }
                            .collect { emit(it) }
                    }
                }
                emit(StageCompleteMessage(stage, duration))
            }
        }
    }
}

fun produceWork(workloads: NonEmptyList<WeightedWorkload>, stageState: State, client: MongoClient): Flow<ExecutionContext> = flow {
    val weights = workloads.map { it.weight }.toMutableList()
    val counts = workloads.map { it.count }.toMutableList()

    var weightSum = weights.sum()
    while (weightSum > 0) {
        val selection = Random.nextElementIndex(weights, weightSum).let {
            val count = ++counts[it]
            if (count == 0L) {
                weights[it] = 0
                weightSum = weights.sum()
            }
            workloads[it]
        }

//        emit(selection)
//        rate.delay()
    }
}

fun produceWork(workload: Workload, stageState: State, client: MongoClient): Flow<ExecutionContext> = flow {
    var context = workload.createContext(client, stageState)
    for (i in 1..workload.count) {
        context = context.withState(context.state.copy(executionCount = i))
        emit(context)
        delay(context)
    }
}
