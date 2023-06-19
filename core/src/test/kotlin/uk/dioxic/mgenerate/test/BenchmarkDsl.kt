package uk.dioxic.mgenerate.test

import kotlinx.serialization.json.put
import uk.dioxic.mgenerate.buildTemplate
import uk.dioxic.mgenerate.worker.model.*
import kotlin.time.Duration

val defaultTemplate = buildTemplate {
    put("name", "\$name")
//    put("date", LocalDateTime.now())
//    put("uuid", UUID.randomUUID())
//    put("long", Long.MAX_VALUE)
}

val defaultExecutor = InsertOneExecutor(
    database = "myDB",
    collection = "myCollection",
    template = defaultTemplate
)

fun benchmark(name: String = "benchmark", init: BenchmarkBuilder.() -> Unit): Benchmark {
    val builder = BenchmarkBuilder(name)
    builder.init()
    return builder.build()
}

class BenchmarkBuilder(private val name: String) {
    private val stages = mutableListOf<Stage>()

    fun sequentialStage(name: String? = null, init: SequentialStageBuilder.() -> Unit) {
        val builder = SequentialStageBuilder(name ?: "stage${stages.size}")
        builder.init()
        stages.add(builder.build())
    }

    fun parallelStage(
        name: String? = null,
        timeout: Duration = Duration.INFINITE,
        init: ParallelStageBuilder.() -> Unit
    ) {
        val builder = ParallelStageBuilder(name ?: "stage${stages.size}", timeout)
        builder.init()
        stages.add(builder.build())
    }

    fun build() =
        Benchmark(name = name, stages = stages)
}

abstract class StageBuilder(protected val name: String) {
    protected val workloads = mutableListOf<Workload>()

    fun rateWorkload(
        executor: Executor<*> = defaultExecutor,
        name: String? = null,
        count: Long = 1,
        rate: Rate = Unlimited,
    ) {
        workloads.add(
            RateWorkload(
                name = name ?: "workload${workloads.size}",
                count = count,
                rate = rate,
                executor = executor
            )
        )
    }

    abstract fun build(): Stage
}

class SequentialStageBuilder(name: String) : StageBuilder(name) {
    override fun build() =
        SequentialStage(name = name, workloads = workloads)
}

class ParallelStageBuilder(
    name: String,
    private val timeout: Duration
) : StageBuilder(name) {

    fun weightedWorkload(
        executor: Executor<*> = defaultExecutor,
        name: String? = null,
        weight: Int = 1,
        count: Long = 1,
    ) {
        workloads.add(
            WeightedWorkload(
                name = name ?: "workload${workloads.size}",
                count = count,
                weight = weight,
                executor = executor
            )
        )
    }

    override fun build() =
        ParallelStage(name = name, timeout = timeout, workloads = workloads)
}

//class WorkloadBuilder(val name: String, val weight: Int, val rate: Rate, val count: Long) {
//    var executor: Executor? = null
//
//    fun insertOne(database: String, collection: String, template: String) {
//        executor = InsertOneExecutor(database, collection, template)
//    }
//
//    fun build(): Workload {
//        executor.let {
//            require(it != null) { "executor not set!" }
//            return Workload(name, it)
//        }
//    }
//}