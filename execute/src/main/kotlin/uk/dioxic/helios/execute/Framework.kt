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
//                        .onEach {
//                            println("first state: ${it.stateContext.first().variables.value }")
//                        }
                        .parMapUnordered(concurrency) { execution ->
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
    val stateFlow: Flow<StateContext> = flow {
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
        } else {
            it
        }
    }

    return when (stage) {
        is SequentialStage -> {
            stage.workloads.asFlow().flatMapConcat { workload ->
                workload.toCtx().zip(stateFlow)
            }
        }

        is ParallelStage -> buildList {
            val rateWorkloads = stage.workloads.filterIsInstance<RateWorkload>()
            val weightedWorkloads = stage.workloads.filterIsInstance<WeightedWorkload>()

            addAll(rateWorkloads.map { it.toCtx().zip(stateFlow) })
            add(weightedWorkloads.toCtx(stage.weightedWorkloadRate).zip(stateFlow))
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

fun List<WeightedWorkload>.toCtx(rate: Rate): Flow<WorkloadContext> = flow {
    val weights = map { it.weight }.toMutableList()
    val counts = MutableList(size) { 0L }
    var weightSum = weights.sum()
    while (weightSum > 0) {
        emit(Random.nextElementIndex(weights, weightSum).let {
            val count = ++counts[it]
            if (count == get(it).count) {
                weights[it] = 0
                weightSum = weights.sum()
            }
            get(it) to count
        })
    }
}.map { (workload, executionId) ->
    WorkloadContext(
        workload = workload,
        rate = rate,
        executionId = executionId,
    )
}

fun RateWorkload.toCtx(): Flow<WorkloadContext> =
    (1..count).asFlow().map { executionId ->
        WorkloadContext(
            workload = this@toCtx,
            rate = rate,
            executionId = executionId,
        )
    }

@OptIn(ExperimentalCoroutinesApi::class)
fun Flow<WorkloadContext>.zip(stateFlow: Flow<StateContext>): Flow<ExecutionContext> {
    return flatMapConcat { wCtx ->
        (0 until wCtx.workload.executor.variablesRequired)
            .map { wCtx }
            .asFlow()
    }.zip(stateFlow) { wCtx, sCtx ->
        wCtx to sCtx.copy(
            constants = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
                sCtx.constants.value + wCtx.workload.constants.value
            },
            variables = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
                sCtx.variables.value + wCtx.workload.variables
            },
        )
//    }.onEach {
//        println("ctx pairs: (workload: ${it.first.workload.name}, wkExId: ${it.first.executionId}, stateId: ${it.second.count})," +
//                "stateVars: ${it.second.variables.value}")
    }.groupBy({ it.workload to it.executionId }) { wCtx, sCtxList ->
        ExecutionContext.create(wCtx, sCtxList)
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