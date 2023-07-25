package uk.dioxic.helios.execute

import arrow.fx.coroutines.parMapUnordered
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.bson.Bson
import okio.FileSystem
import okio.buffer
import org.bson.Document
import uk.dioxic.helios.execute.model.*
import uk.dioxic.helios.execute.resources.ResourceRegistry
import uk.dioxic.helios.execute.results.*
import uk.dioxic.helios.generate.StateContext
import uk.dioxic.helios.generate.extensions.nextElementIndex
import uk.dioxic.helios.generate.flatten
import uk.dioxic.helios.generate.hydrate
import uk.dioxic.helios.generate.serialization.DocumentSerializer
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlin.time.measureTime
import okio.Path as OkioPath

/**
 * @param interval the message batching/summarization interval (0 to disable batching)
 */
@OptIn(FlowPreview::class)
fun Benchmark.execute(
    registry: ResourceRegistry = ResourceRegistry.EMPTY,
    concurrency: Int = 4,
    interval: Duration = 1.seconds
): Flow<FrameworkMessage> = flow {
    with(registry) {
        stages.forEach { stage ->
            emit(StageStartMessage(stage))
            val duration = measureTime {
                withTimeoutOrNull(stage.timeout) {
                    produceExecutions(this@execute, stage)
                        .buffer(100)
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

//context(ResourceRegistry)
//fun produceStateFlow(
//    benchmark: Benchmark,
//    stage: Stage
//): Flow<StateContext> {
//    var count = 0L
//    return benchmark.dictionaries.asFlow().zip(stage.dictionaries.asFlow()) { b, s ->
//        with(StateContext(dictionaries = b + s)) {
//            copy(
//                variables = (benchmark.variables + stage.variables).hydrate().flatten(),
//                count = ++count
//            )
//        }
//    }.buffer(100)
//}

context(ResourceRegistry)
@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
fun produceExecutions(
    benchmark: Benchmark,
    stage: Stage
): Flow<ExecutionContext> {
    var count = 0L
    val stateFlow = benchmark.dictionaries.asFlow().zip(stage.dictionaries.asFlow()) { b, s ->
        with(StateContext(dictionaries = b + s)) {
            copy(
                variables = (benchmark.variables + stage.variables).hydrate().flatten(),
                count = ++count
            )
        }
    }.buffer(100)

    return when (stage) {
        is SequentialStage -> {
            stage.workloads.asFlow().flatMapConcat { workload ->
                workload.toCtx().zip(stateFlow)
            }
        }

        is ParallelStage -> buildList {
            val sharedFlow = stateFlow.shareIn(
                scope = GlobalScope,
                started = SharingStarted.StartWhenSubscribedAtLeast(stage.subscriberCount()),
                replay = 0
            )
            val rateWorkloads = stage.workloads.filterIsInstance<RateWorkload>()
            val weightedWorkloads = stage.workloads.filterIsInstance<WeightedWorkload>()

            addAll(rateWorkloads.map { it.toCtx().zip(sharedFlow) })
            add(weightedWorkloads.toCtx(stage.weightedWorkloadRate).zip(sharedFlow))
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
fun Flow<WorkloadContext>.zip(stateFlow: Flow<StateContext>): Flow<ExecutionContext> =
    flatMapConcat { wCtx ->
        (0 until wCtx.workload.executor.variablesRequired)
            .map { wCtx }
            .asFlow()
    }.zip(stateFlow) { wCtx, sCtx ->
        wCtx to sCtx.copy(
            variables = with(sCtx) {
                (sCtx.variables + wCtx.workload.variables).hydrate().flatten()
            },
        )
    }.groupBy({ it.workload to it.executionId }) { wCtx, sCtxList ->
        ExecutionContext.create(wCtx, sCtxList)
    }.onEach {
        it.delay()
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

/**
 * Returns a flow of dictionary values keyed on the dictionary name.
 */
context (ResourceRegistry)
fun Dictionaries.asFlow(fileSystem: FileSystem = FileSystem.SYSTEM): Flow<HydratedDictionaries> =
    map { (k, v) ->
        v.asResourcedFlow(k, fileSystem).map { mapOf(k to it) }
    }.reduceOrNull { acc, flow ->
        acc.zip(flow) { t1, t2 ->
            t1 + t2
        }
    } ?: flow {
        while (true) {
            emit(emptyMap())
        }
    }

/**
 * If the dictionary has storage enabled and a store file exists, read from the file.
 * Otherwise, use the default flow for the dictionary.
 *
 * @param key the dictionary key
 * @param fileSystem the Okio file system to use
 * @return a hydrated flow of dictionary values
 */
context (ResourceRegistry)
fun Dictionary.asResourcedFlow(key: String, fileSystem: FileSystem = FileSystem.SYSTEM): Flow<HydratedDictionary> {
    val okioPath = store.getOkioPath(key)

    return if (okioPath != null && fileSystem.exists(okioPath)) {
        okioPath.asFlow(fileSystem)
    } else {
        asFlow()
    }
}

/**
 * Returns an infinite flow of documents from a file.
 *
 * Once all lines have been processed, the file is reset to the beginning.
 * @param fileSystem the [FileSystem] to use
 * @return flow of [Document]
 */
fun OkioPath.asFlow(fileSystem: FileSystem = FileSystem.SYSTEM): Flow<Document> = flow {
    fileSystem.openReadOnly(this@asFlow).use { handle ->
        val source = handle.source().buffer()

        var count = 0L
        do {
            while (true) {
                val line = source.readUtf8Line() ?: break
                emit(Bson.decodeFromString(DocumentSerializer, line))
                count++
            }
            handle.reposition(source, 0)
        } while (count > 0)
    }
}