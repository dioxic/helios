package uk.dioxic.helios.execute

import arrow.fx.coroutines.parMapUnordered
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import uk.dioxic.helios.execute.model.*
import uk.dioxic.helios.execute.resources.ResourceRegistry
import uk.dioxic.helios.execute.results.*
import uk.dioxic.helios.generate.StateContext
import uk.dioxic.helios.generate.extensions.nextElementIndex
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlin.time.measureTime

/**
 * @param interval the message batching/summarization interval (0 to disable batching)
 */
@OptIn(FlowPreview::class)
fun Benchmark.execute(
    registry: ResourceRegistry = ResourceRegistry(),
    concurrency: Int = 4,
    interval: Duration = 1.seconds,
    linkedVariables: Boolean = false
): Flow<FrameworkMessage> = flow {
    with(registry) {
        stages.forEach { stage ->
            emit(StageStartMessage(stage))
            val duration = measureTime {
                withTimeoutOrNull(stage.timeout) {
                    produceExecutions(this@execute, stage, linkedVariables)
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

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
fun produceExecutions(
    benchmark: Benchmark,
    stage: Stage,
    linkedVariables: Boolean
): Flow<ExecutionContext> {
    val ctxFlow: Flow<StateContext> = flow {
        val constants = lazy { benchmark.constants.value + stage.constants.value }
        var count = 0L
        while (true) {
            emit(
                StateContext(
                    variables = lazy { benchmark.variables + stage.variables },
                    constants = constants,
                    count = ++count
                )
            )
        }
    }.let {
        if (linkedVariables) {
            it.buffer(100).shareIn(
                scope = GlobalScope,
                started = SharingStarted.StartWhenSubscribedAtLeast(stage.subscriberCount()),
                replay = 0
            )
        }
        else {
            it
        }
    }

    return when (stage) {
        is SequentialStage -> {
            stage.workloads.asFlow().flatMapConcat { workload ->
                ctxFlow.zip(workload)
            }
        }

        is ParallelStage -> buildList {
            val rateWorkloads = stage.workloads.filterIsInstance<RateWorkload>()
            val weightedWorkloads = stage.workloads.filterIsInstance<WeightedWorkload>()

            addAll(rateWorkloads.map { ctxFlow.zip(it) })
            add(ctxFlow.zip(weightedWorkloads, stage.weightedWorkloadRate))
        }.merge()
    }.flowOn(Dispatchers.Default)
}

fun Stage.subscriberCount(): Int =
    workloads.size - workloads.filterIsInstance<WeightedWorkload>().count().let { count ->
        if (count > 0) {
            count - 1
        } else {
            count
        }
    }

fun Flow<StateContext>.zip(workloads: List<WeightedWorkload>, rate: Rate): Flow<ExecutionContext> {
    val workloadFlow = flow {
        val weights = workloads.map { it.weight }.toMutableList()
        val counts = MutableList(workloads.size) { 0L }
        var weightSum = weights.sum()
        while (weightSum > 0) {
            emit(Random.nextElementIndex(weights, weightSum).let {
                val count = ++counts[it]
                if (count == workloads[it].count) {
                    weights[it] = 0
                    weightSum = weights.sum()
                }
                workloads[it] to count
            })

        }
    }

    return zip(workloadFlow) { ctx, (workload, i) ->
        ExecutionContext(
            workload = workload,
            rate = rate,
            constants = lazy(LazyThreadSafetyMode.NONE) {
                ctx.constants.value + workload.constants.value
            },
            variables = lazy(LazyThreadSafetyMode.NONE) {
                ctx.variables.value + workload.variables
            },
            count = i,
        )
    }.onEach {
        it.delay()
    }
}

fun Flow<StateContext>.zip(workload: RateWorkload): Flow<ExecutionContext> {
    return zip((1..workload.count).asFlow()) { ctx, i ->
        require(i == ctx.count) {
            "something has gone wrong expected count [$i] to be the same as the context count [${ctx.count}]"
        }
        ExecutionContext(
            workload = workload,
            rate = workload.rate,
            constants = lazy(LazyThreadSafetyMode.NONE) {
                ctx.constants.value + workload.constants.value
            },
            variables = lazy(LazyThreadSafetyMode.NONE) {
                ctx.variables.value + workload.variables
            },
            count = i,
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