package uk.dioxic.helios.execute

import arrow.fx.coroutines.parMapUnordered
import arrow.fx.coroutines.resourceScope
import com.mongodb.MongoException
import jdk.incubator.foreign.ResourceScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.bson.Bson
import okio.BufferedSink
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import org.bson.Document
import uk.dioxic.helios.execute.model.*
import uk.dioxic.helios.execute.resources.ResourceRegistry
import uk.dioxic.helios.execute.resources.fileSink
import uk.dioxic.helios.execute.results.ExecutionResult
import uk.dioxic.helios.execute.results.TimedExceptionResult
import uk.dioxic.helios.execute.results.TimedExecutionResult
import uk.dioxic.helios.execute.results.TimedResult
import uk.dioxic.helios.generate.extensions.nextElementIndex
import uk.dioxic.helios.generate.flatten
import uk.dioxic.helios.generate.hydrate
import uk.dioxic.helios.generate.serialization.DocumentSerializer
import uk.dioxic.helios.generate.serialization.GenericMapSerializer
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
@OptIn(FlowPreview::class, DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
fun Benchmark.execute(
    registry: ResourceRegistry = ResourceRegistry.EMPTY,
    concurrency: Int = 4,
    interval: Duration = 1.seconds
): Flow<FrameworkMessage> = flow {
    with(registry) {
        stages.filterNot { it.disable }.forEach { stage ->
            emit(StageStartMessage(stage))
            resourceScope {
                val dictionarySinks = createDictionaryConsumers(this@execute, stage)
                println("sinks: ${dictionarySinks.size}")
                val duration = measureTime {
                    withTimeoutOrNull(stage.timeout) {
                        println("starting producing....")
                        produceExecutions(this@execute, stage)
                            .buffer(100)
                            .parMapUnordered(concurrency) { execution ->
                                execution.invoke()
                            }.onEach {
                                dictionarySinks.writeState(it.context.stateContext)
                                it
                            }
                            .chunked(interval)
                            .map { ProgressMessage(stage, it) }
                            .collect { emit(it) }
                    }
                }
                emit(StageCompleteMessage(stage, duration))
            }
        }
    }
}.flowOn(Dispatchers.Default)

context(ResourceRegistry)
fun produceStateFlow(
    benchmark: Benchmark,
    stage: Stage
): Flow<StateContext> {
    var count = 0L
    return benchmark.dictionaries.asPersistedFlow().zip(stage.dictionaries.asPersistedFlow()) { b, s ->
        StateContext(
            dictionaryList = b.values + s.values,
            dictionaries = (b + s).flatten(),
            count = ++count
        ).run {
            copy(
                variables = (benchmark.variables + stage.variables).hydrate(),
            )
        }
    }.buffer(100)
}

context(ResourceRegistry)
@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
fun produceExecutions(
    benchmark: Benchmark,
    stage: Stage
): Flow<ExecutionContext> {
    val stateFlow = produceStateFlow(benchmark, stage)

    return when (stage) {
        is SequentialStage -> {
            stage.workloads.filterNot { it.disable }.asFlow().flatMapConcat { workload ->
                workload.asFlow().zip(stateFlow).mapToExecutionContext()
            }
        }

        is ParallelStage -> buildList {
            val sharedStateFlow = stateFlow.shareIn(
                scope = GlobalScope,
                started = SharingStarted.StartWhenSubscribedAtLeast(stage.subscriberCount()),
                replay = 0
            )
            val enabledWorkloads = stage.workloads.filterNot { it.disable }
            val rateWorkloads = enabledWorkloads.filterIsInstance<RateWorkload>()
            val weightedWorkloads = enabledWorkloads.filterIsInstance<WeightedWorkload>()

            addAll(rateWorkloads.map { it.asFlow().zip(sharedStateFlow).mapToExecutionContext() })
            add(weightedWorkloads.asFlow(stage.weightedWorkloadRate).zip(sharedStateFlow).mapToExecutionContext())
        }.merge()
    }.flowOn(Dispatchers.Default)
}

fun ParallelStage.subscriberCount(): Int =
    workloads.size - workloads.filterIsInstance<WeightedWorkload>().count().let { count ->
        if (count > 0) {
            count - 1
        } else {
            count
        }
    }

private fun List<WeightedWorkload>.asFlow(rate: Rate): Flow<WorkloadContext> = flow {
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

private fun RateWorkload.asFlow(): Flow<WorkloadContext> =
    (1..count).asFlow().map { executionId ->
        WorkloadContext(
            workload = this@asFlow,
            rate = rate,
            executionId = executionId,
        )
    }

@OptIn(ExperimentalCoroutinesApi::class)
private fun Flow<WorkloadContext>.unwindOnModels() =
    flatMapConcat { wCtx ->
        (0 until wCtx.workload.executor.modelSize)
            .map { wCtx }
            .asFlow()
    }

/**
 * Zips workload models and state contexts together.
 * For example, an InsertMany with 10 documents will result in 10x output pairs.
 */
fun Flow<WorkloadContext>.zip(stateFlow: Flow<StateContext>): Flow<Pair<WorkloadContext, StateContext>> =
    unwindOnModels().zip(stateFlow) { wCtx, sCtx ->
        wCtx to sCtx.copy(
            variables = with(sCtx) {
                (sCtx.variables + wCtx.workload.variables).hydrate().flatten()
            },
        )
    }

fun Flow<Pair<WorkloadContext, StateContext>>.mapToExecutionContext(): Flow<ExecutionContext> =
    groupBy({ it.workload to it.executionId }) { wCtx, sCtxList ->
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
    return try {
        TimedExecutionResult(block(), mark.elapsedNow(), this)
    } catch (e: MongoException) {
        TimedExceptionResult(e, mark.elapsedNow(), this)
    }
}

/**
 * Returns a flow of hydrated dictionaries keyed on the dictionary name.
 */
context (ResourceRegistry)
fun Dictionaries.asPersistedFlow(fileSystem: FileSystem = FileSystem.SYSTEM): Flow<Map<String, HydratedDictionary>> =
    map { (k, v) ->
        v.asPersistedFlow(k, fileSystem).map { mapOf(it.key to it) }
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

context(ResourceRegistry)
private fun List<DictionarySink>.writeState(states: List<StateContext>) {
    forEach { dc ->
        states.mapNotNull {
            val dicFind = it.dictionaryList.firstOrNull { d -> d.definition == dc.dictionary }
            println("dicFind: $dicFind")
            dicFind
        }.forEach { values ->
            println("writing state")
            dc.sink.writeUtf8(Bson.encodeToString(GenericMapSerializer, values))
            dc.sink.writeUtf8("/n")
        }
    }
}

data class DictionarySink(
    val dictionary: Dictionary,
    val sink: BufferedSink,
//    private val path: OkioPath,
) {
//    @OptIn(ExperimentalCoroutinesApi::class)
//    suspend fun consume(stateFlow: Flow<ExecutionContext>, fileSystem: FileSystem = FileSystem.SYSTEM) {
//        fileSystem.openReadWrite(path).use { handle ->
//            val sink = handle.sink().buffer()
//
//            stateFlow
//                .flatMapConcat { it.stateContext.asFlow() }
//                .mapNotNull { sCtx -> sCtx.dictionaryList.firstOrNull { d -> d.definition == dictionary } }
//                .collect {
//                    sink.writeUtf8(Bson.encodeToString(GenericMapSerializer, it))
//                    sink.writeUtf8("/n")
//                }
//        }
//    }
}

/**
 * Grab all the persistable-dictionaries where the path does NOT exist and convert
 * to a list of [DictionarySink]
 * This is used to determine which dictionaries are reading vs writing.
 */
suspend fun ResourceScope.createDictionaryConsumers(
    benchmark: Benchmark,
    stage: Stage,
    fileSystem: FileSystem = FileSystem.SYSTEM
): List<DictionarySink> =
    (benchmark.dictionaries + stage.dictionaries)
        .filter { (k, d) -> d.store.persist && d.store.exists(k) }
        .map { (k, d) -> d to d.store.getOkioPath(k) }
        .map { (d, p) ->
            println("creating dictionary sink for $p")
            DictionarySink(d, fileSink(fileSystem, p))
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
fun Dictionary.asPersistedFlow(key: String, fileSystem: FileSystem = FileSystem.SYSTEM): Flow<HydratedDictionary> =
    if (store.persist && store.exists(key, fileSystem)) {
        store.getOkioPath(key).asFlow(fileSystem)
    } else {
        asFlow()
    }.map {
        HydratedDictionary(key, this, it)
    }

fun Store.getOkioPath(key: String): Path =
    when (this) {
        is PathStore -> path.toPath()
        is BooleanStore -> {
            if (persist) {
                "$key.$defaultStoreExtension".toPath()
            } else {
                throw IllegalArgumentException("non-persistent store doesn't have a path")
            }
        }
    }

fun Store.exists(key: String, fileSystem: FileSystem = FileSystem.SYSTEM): Boolean =
    fileSystem.exists(getOkioPath(key))