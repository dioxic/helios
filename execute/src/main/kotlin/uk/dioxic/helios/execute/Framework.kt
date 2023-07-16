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

typealias SharedVarsFlow = SharedFlow<Lazy<Map<String, Any?>>>

/**
 * @param interval the message batching/summarization interval (0 to disable batching)
 */
@OptIn(FlowPreview::class)
fun Benchmark.execute(
    registry: ResourceRegistry = ResourceRegistry(),
    concurrency: Int = 4,
    interval: Duration = 1.seconds,
    varBufferSize: Int = 100
): Flow<FrameworkMessage> = flow {
    with(registry) {
        stages.forEach { stage ->
            emit(StageStartMessage(stage))
            val duration = measureTime {
                withTimeoutOrNull(stage.timeout) {
                    stage.produceExecutions(this@execute, varBufferSize)
                        .buffer(100)
                        .parMapUnordered(concurrency) { execution ->
//                            println("variables: ${execution.variables.value}")
                            execution.invoke()
                        }
                        .chunked(interval)
                        .map { ProgressMessage(stage, it) }
                        .collect { emit(it) }
                }
            }
            emit(StageCompleteMessage(stage, duration))
        }
    }
}.flowOn(Dispatchers.Default)

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
fun Stage.produceExecutions(
    benchmark: Benchmark = Benchmark(name = "benchmark", stages = listOf(this)),
    varBufferSize: Int
): Flow<ExecutionContext> {
    val variablesFlow = flow {
        while (true) {
            emit(lazy { benchmark.variables + variables })
        }
    }.shareIn(
        scope = GlobalScope,
        started = SharingStarted.Eagerly,
        replay = varBufferSize
    )

    return when (this) {
        is SequentialStage -> {
            workloads.asFlow().flatMapConcat { workload ->
                produceRated(benchmark, workload, variablesFlow)
            }
        }

        is ParallelStage -> buildList {
            val rateWorkloads = workloads.filterIsInstance<RateWorkload>()
            val weightedWorkloads = workloads.filterIsInstance<WeightedWorkload>()

            addAll(rateWorkloads.map { produceRated(benchmark, it, variablesFlow) })
            add(produceWeighted(benchmark, weightedWorkloads, variablesFlow))
        }.merge()
    }.flowOn(Dispatchers.Default)
}

fun Stage.produceWeighted(
    benchmark: Benchmark,
    workloads: List<WeightedWorkload>,
    variables: SharedVarsFlow
): Flow<ExecutionContext> {

    val randomContext = flow {
        val contexts = workloads.map { it.createContext(benchmark, this@produceWeighted) }
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

fun Stage.produceRated(
    benchmark: Benchmark,
    workload: RateWorkload,
    variables: SharedVarsFlow
): Flow<ExecutionContext> {
    val context = workload.createContext(benchmark, this)

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