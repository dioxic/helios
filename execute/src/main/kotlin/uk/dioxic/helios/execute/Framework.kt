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

typealias SharedVarsFlow = SharedFlow<Lazy<Map<String,Any?>>>

/**
 * @param interval the message batching/summarization interval (0 to disable batching)
 */
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
                        .parMapUnordered(concurrency) { execution -> execution.invoke() }
                        .chunked(interval)
                        .map { ProgressMessage(stage, it) }
                        .collect { emit(it) }
                }
            }
            emit(StageCompleteMessage(stage, duration))
        }
    }
}.flowOn(Dispatchers.Default)

context(CoroutineScope)
@OptIn(DelicateCoroutinesApi::class)
fun Benchmark.produceExecutions(stage: Stage, variablesBufferSize: Int = 100): Flow<ExecutionContext> {
    val variablesFlow = flow {
        while (true) {
            emit(lazy { variables + stage.variables })
        }
    }.shareIn(
        scope = GlobalScope,
        started = SharingStarted.Eagerly,
        replay = variablesBufferSize
    )

    return when (stage) {
        is SequentialStage -> produceSequential(stage, variablesFlow)
        is ParallelStage -> produceParallel(stage, variablesFlow)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun Benchmark.produceSequential(
    stage: SequentialStage,
    variables: SharedVarsFlow
): Flow<ExecutionContext> =
    stage.workloads.asFlow().flatMapConcat { workload ->
        produceRated(stage, workload, variables)
    }.flowOn(Dispatchers.Default)

fun Benchmark.produceParallel(
    stage: ParallelStage,
    variables: SharedVarsFlow
): Flow<ExecutionContext> = buildList {
    val rateWorkloads = stage.workloads.filterIsInstance<RateWorkload>()
    val weightedWorkloads = stage.workloads.filterIsInstance<WeightedWorkload>()

    addAll(rateWorkloads.map { produceRated(stage, it, variables) })
    add(produceWeighted(stage, weightedWorkloads, variables))
}.merge().flowOn(Dispatchers.Default)

fun Benchmark.produceWeighted(
    stage: Stage,
    workloads: List<WeightedWorkload>,
    variables: SharedVarsFlow
): Flow<ExecutionContext> {

    val randomContext = flow {
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
                contexts[it] to count
            }.also {
                emit(it)
                it.first.delay()
            }
        }
    }

    return randomContext
        .zip(variables) { (ctx, i), v ->
            ctx.copy(
                executionCount = i,
                variables = lazy(LazyThreadSafetyMode.NONE) {
                    v.value + ctx.workload.variables
                }
            )
        }
}

fun Benchmark.produceRated(
    stage: Stage,
    workload: RateWorkload,
    variables: SharedVarsFlow
): Flow<ExecutionContext> {
    val context = workload.createContext(this@produceRated, stage)

    return (1..context.workload.count).asFlow()
        .zip(variables) { i, v ->
            context.copy(
                executionCount = i,
                variables = lazy(LazyThreadSafetyMode.NONE) {
                    v.value + workload.variables
                }
            )
        }.onEach {
            it.delay()
        }
}

//private fun getLazyVariables(benchmark: Benchmark, stage: Stage, workload: Workload, context: ExecutionContext) =
//    lazy(LazyThreadSafetyMode.NONE) {
//        val executor = context.executor
//        if (executor is Stateful) {
//            benchmark.variables + stage.variables + workload.variables + executor.variables
//        } else {
//            benchmark.variables + stage.variables + workload.variables
//        }
//    }

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