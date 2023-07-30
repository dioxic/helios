@file:Suppress("MemberVisibilityCanBePrivate")

package uk.dioxic.helios.execute

import uk.dioxic.helios.execute.model.*
import uk.dioxic.helios.generate.Template
import kotlin.time.Duration

@DslMarker
annotation class BenchmarkMarker

fun buildBenchmark(
    name: String = "benchmark",
    block: BenchmarkBuilder.() -> Unit
): Benchmark {
    val builder = BenchmarkBuilder(name)
    builder.block()
    return builder.build()
}

fun buildSequentialStage(init: SequentialStageBuilder.() -> Unit): SequentialStage {
    val builder = SequentialStageBuilder("sequential")
    builder.init()
    return builder.build()
}

fun buildParallelStage(init: ParallelStageBuilder.() -> Unit): ParallelStage {
    val builder = ParallelStageBuilder("parallel")
    builder.init()
    return builder.build()
}

@BenchmarkMarker
class BenchmarkBuilder(
    var name: String,
) {
    var dictionaries: Dictionaries = emptyMap()
    var variables: Template = Template.EMPTY
    private val stages = mutableListOf<Stage>()

    fun sequentialStage(block: SequentialStageBuilder.() -> Unit) {
        val builder = SequentialStageBuilder("stage${stages.size}")
        builder.block()
        stages.add(builder.build())
    }

    fun parallelStage(block: ParallelStageBuilder.() -> Unit) {
        val builder = ParallelStageBuilder("stage${stages.size}")
        builder.block()
        stages.add(builder.build())
    }

    fun build() = Benchmark(
        name = name,
        stages = stages,
        dictionaries = dictionaries,
        variables = variables,
    )
}

@BenchmarkMarker
class SequentialStageBuilder(var name: String) {
    var sync: Boolean = false
    var dictionaries: Dictionaries = emptyMap()
    var variables: Template = Template.EMPTY
    private val workloads = mutableListOf<RateWorkload>()

    fun addRateWorkload(
        name: String? = null,
        executor: Executor,
        count: Long = 1,
        variables: Template = Template.EMPTY,
        rate: Rate = UnlimitedRate,
    ) {
        workloads.add(
            RateWorkload(
                name = name ?: "workload${workloads.size}",
                count = count,
                rate = rate,
                variables = variables,
                executor = executor
            )
        )
    }

    fun build() = SequentialStage(
        name = name,
        sync = sync,
        workloads = workloads,
        dictionaries = dictionaries,
        variables = variables,
    )
}

@BenchmarkMarker
class ParallelStageBuilder(var name: String) {
    var sync: Boolean = false
    var timeout: Duration = Duration.INFINITE
    var dictionaries: Dictionaries = emptyMap()
    var variables: Template = Template.EMPTY
    private val workloads = mutableListOf<Workload>()

    fun addRateWorkload(
        name: String? = null,
        executor: Executor,
        count: Long = 1,
        variables: Template = Template.EMPTY,
        rate: Rate = UnlimitedRate,
    ) {
        workloads.add(
            RateWorkload(
                name = name ?: "workload${workloads.size}",
                count = count,
                rate = rate,
                variables = variables,
                executor = executor
            )
        )
    }

    fun addWeightedWorkload(
        name: String? = null,
        executor: Executor,
        weight: Int = 1,
        variables: Template = Template.EMPTY,
        count: Long = 1,
    ) {
        workloads.add(
            WeightedWorkload(
                name = name ?: "workload${workloads.size}",
                count = count,
                weight = weight,
                variables = variables,
                executor = executor
            )
        )
    }

    fun build() = ParallelStage(
        name = name,
        sync = sync,
        timeout = timeout,
        workloads = workloads,
        dictionaries = dictionaries,
        variables = variables,
    )
}