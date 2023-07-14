package uk.dioxic.helios.execute

import arrow.fx.coroutines.parMapUnordered
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import uk.dioxic.helios.execute.model.*
import uk.dioxic.helios.execute.resources.ResourceRegistry
import uk.dioxic.helios.execute.results.*
import uk.dioxic.helios.generate.extensions.nextElementIndex
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlin.time.measureTime

@OptIn(FlowPreview::class)
fun Benchmark.execute(
    registry: ResourceRegistry = ResourceRegistry(),
    concurrency: Int = 4,
    interval: Duration = 1.seconds,
): Flow<FrameworkMessage> = flow {
    with(registry) {
        stages.forEach { stage ->
            emit(StageStartMessage(stage))
            val duration = measureTime {
                withTimeoutOrNull(stage.timeout) {
                    produceExecutions(stage)
                        .buffer(100)
                        .parMapUnordered(concurrency) { it() }
                        .chunked(interval)
                        .map { ProgressMessage(stage, it) }
                        .collect { emit(it) }
                }
            }
            emit(StageCompleteMessage(stage, duration))
        }
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
    }.flowOn(Dispatchers.Default)

fun Benchmark.produceParallel(
    stage: ParallelStage,
): Flow<ExecutionContext> = buildList {
    val rateWorkloads = stage.workloads.filterIsInstance<RateWorkload>()
    val weightedWorkloads = stage.workloads.filterIsInstance<WeightedWorkload>()

    addAll(rateWorkloads.map { produceRated(stage, it) })
    add(produceWeighted(stage, weightedWorkloads))
}.merge().flowOn(Dispatchers.Default)

fun Benchmark.produceWeighted(
    stage: Stage,
    workloads: List<WeightedWorkload>
): Flow<ExecutionContext> = flow {
    val contexts = workloads.map { it.createContext(this@produceWeighted, stage) }
    val weights = workloads.map { it.weight }.toMutableList()
    val counts = MutableList(workloads.size) { 0L }

    var weightSum = weights.sum()
    while (weightSum > 0) {
        Random.nextElementIndex(weights, weightSum).let {
            val count = ++counts[it]
            if (count == workloads[it].count) {
                weights[it] = 0
                weightSum = weights.sum()
            }
            val variables = getLazyVariables(
                benchmark = this@produceWeighted,
                stage = stage,
                workload = workloads[it],
                context = contexts[it]
            )
            contexts[it].copy(
                executionCount = count,
                variables = variables
            )
        }.also {
            emit(it)
            it.delay()
        }
    }
}

fun Benchmark.produceRated(
    stage: Stage,
    workload: RateWorkload
): Flow<ExecutionContext> = flow {
    val context = workload.createContext(this@produceRated, stage)
    for (i in 1..context.workload.count) {
        val variables = getLazyVariables(
            benchmark = this@produceRated,
            stage = stage,
            workload = workload,
            context = context
        )
        context.copy(
            executionCount = i,
            variables = variables
        ).also {
            emit(it)
            it.delay()
        }
    }
}

private fun getLazyVariables(benchmark: Benchmark, stage: Stage, workload: Workload, context: ExecutionContext) =
    lazy(LazyThreadSafetyMode.NONE) {
        val executor = context.executor
        if (executor is Stateful) {
            benchmark.variables + stage.variables + workload.variables + executor.variables
        } else {
            benchmark.variables + stage.variables + workload.variables
        }
    }

suspend fun ExecutionContext.delay() {
    val delay = this.rate.calculateDelay()
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

inline fun ExecutionContext.measureTimedResult(block: () -> ExecutionResult): TimedResult {
    val mark = TimeSource.Monotonic.markNow()

    return when (val value = block()) {
        is WriteResult -> TimedWriteResult(value, mark.elapsedNow(), this)
        is ReadResult -> TimedReadResult(value, mark.elapsedNow(), this)
        is MessageResult -> TimedMessageResult(value, mark.elapsedNow(), this)
        is CommandResult -> TimedCommandResult(value, mark.elapsedNow(), this)
        is TransactionResult -> TimedTransactionResult(value, mark.elapsedNow(), this)
        is ErrorResult -> TimedErrorResult(value, mark.elapsedNow(), this)
    }
}