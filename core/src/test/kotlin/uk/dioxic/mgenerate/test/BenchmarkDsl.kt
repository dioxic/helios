package uk.dioxic.mgenerate.test

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import uk.dioxic.mgenerate.OperatorFactory
import uk.dioxic.mgenerate.Template
import uk.dioxic.mgenerate.worker.model.*
import kotlin.time.Duration


fun benchmark(name: String = "benchmark", init: BenchmarkBuilder.() -> Unit): Benchmark {
    val builder = BenchmarkBuilder(name)
    builder.init()
    return builder.build()
}

class BenchmarkBuilder(private val name: String) {
    private val stages = mutableListOf<Stage>()

    fun sequentialStage(name: String? = null, init: SequentialStageBuilder.() -> Unit) {
        val builder = SequentialStageBuilder(name?: "stage${stages.size}")
        builder.init()
        stages.add(builder.build())
    }

    fun parallelStage(
        name: String? = null,
        timeout: Duration = Duration.INFINITE,
        init: ParallelStageBuilder.() -> Unit
    ) {
        val builder = ParallelStageBuilder(name?: "stage${stages.size}", timeout)
        builder.init()
        stages.add(builder.build())
    }

    fun build() =
        Benchmark(name, stages)
}

abstract class StageBuilder(protected val name: String) {
    protected val workloads = mutableListOf<Workload>()

    fun workload(
        executor: Executor = InsertOneExecutor(
            database = "myDB",
            collection = "myCollection",
            template = Template(
                hydratedMap = mapOf(
                    "name" to OperatorFactory.create("\$name"),
                    "long" to Long.MAX_VALUE
                ),
                definition = JsonObject(mapOf(
                    "name" to JsonPrimitive("\$name"),
                    "long" to JsonPrimitive(Long.MAX_VALUE)
                ))
            )
        ),
        name: String? = null,
        weight: Double = 1.0,
        count: Long = 1,
        rate: Rate = Unlimited,
    ) {
        workloads.add(
            Workload(
                name = name ?: "workload${workloads.size}",
                weight = weight,
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
        SequentialStage(name, workloads)
}

class ParallelStageBuilder(
    name: String,
    private val timeout: Duration
) : StageBuilder(name) {
    override fun build() =
        ParallelStage(name, timeout, workloads)
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