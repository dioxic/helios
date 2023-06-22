package uk.dioxic.mgenerate.execute

import arrow.fx.coroutines.parMapUnordered
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import uk.dioxic.mgenerate.execute.model.*
import uk.dioxic.mgenerate.execute.resources.ResourceRegistry
import uk.dioxic.mgenerate.execute.results.*
import uk.dioxic.mgenerate.template.extensions.nextElementIndex
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class, FlowPreview::class)
fun Benchmark.execute(
    registry: ResourceRegistry = ResourceRegistry(),
    concurrency: Int = 4,
    interval: Duration = 1.seconds,
): Flow<FrameworkMessage> = flow {
    stages.forEach { stage ->
        emit(StageStartMessage(stage))
        val duration = measureTime {
            withTimeoutOrNull(stage.timeout) {
                produceExecutions(stage)
                    .buffer(100)
                    .parMapUnordered(concurrency) { it.invoke(registry) }
                    .chunked(interval)
                    .map { ProgressMessage(stage, it) }
                    .collect { emit(it) }
            }
        }
        emit(StageCompleteMessage(stage, duration))
    }
}.flowOn(Dispatchers.Default)

fun Benchmark.produceExecutions(stage: Stage): Flow<ExecutionContext> =
    when (stage) {
        is SequentialStage -> produceSequential(stage)
        is ParallelStage -> produceParallel(stage)
    }

@OptIn(ExperimentalCoroutinesApi::class)
fun Benchmark.produceSequential(
    stage: SequentialStage,
): Flow<ExecutionContext> =
    stage.workloads.asFlow().flatMapConcat { workload ->
        produceRated(stage, workload)
    }

fun Benchmark.produceParallel(
    stage: ParallelStage,
): Flow<ExecutionContext> = buildList {
    val rateWorkloads = stage.workloads.filterIsInstance<RateWorkload>()
    val weightedWorkloads = stage.workloads.filterIsInstance<WeightedWorkload>()

    addAll(rateWorkloads.map { produceRated(stage, it) })
    add(produceWeighted(stage, weightedWorkloads))
}.merge()

fun Benchmark.produceWeighted(
    stage: Stage,
    workloads: List<WeightedWorkload>
): Flow<ExecutionContext> = flow {
    val contexts = workloads.map { it.createContext(this@produceWeighted, stage) }
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

fun Benchmark.produceRated(
    stage: Stage,
    workload: RateWorkload
): Flow<ExecutionContext> = flow {
    val context = workload.createContext(this@produceRated, stage)
    for (i in 1..context.workload.count) {
        context.copy(executionCount = i).also {
            emit(it)
            delay(it)
        }
    }
}

suspend fun delay(context: ExecutionContext) {
    val delay = context.rate.calculateDelay(context)
    when {
        delay == Duration.ZERO -> return
        delay < 100.milliseconds -> {
            val deadline = System.nanoTime() + delay.inWholeNanoseconds
            while (System.nanoTime() < deadline) {
            }
        }

        else -> delay(delay)
    }
}

@OptIn(ExperimentalTime::class)
inline fun measureTimedResult(context: ExecutionContext, block: () -> Result): TimedResult {
    val mark = TimeSource.Monotonic.markNow()
    return when (val value = block()) {
        is WriteResult -> TimedWriteResult(value, mark.elapsedNow(), context)
        is ReadResult -> TimedReadResult(value, mark.elapsedNow(), context)
        is MessageResult -> TimedMessageResult(value, mark.elapsedNow(), context)
        is CommandResult -> TimedCommandResult(value, mark.elapsedNow(), context)
    }
}