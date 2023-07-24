@file:Suppress("MemberVisibilityCanBePrivate")

package uk.dioxic.helios.execute

import uk.dioxic.helios.execute.model.*
import uk.dioxic.helios.generate.Template
import kotlin.time.Duration

fun buildBenchmark(
    name: String = "benchmark",
    init: BenchmarkBuilder.() -> Unit
): Benchmark {
    val builder = BenchmarkBuilder(name)
    builder.init()
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

class BenchmarkBuilder(
    var name: String,
) {
    var dictionaries: Dictionaries = emptyMap()
    var variables: Template = Template.EMPTY
    private val stages = mutableListOf<Stage>()

    fun sequentialStage(init: SequentialStageBuilder.() -> Unit) {
        val builder = SequentialStageBuilder("stage${stages.size}")
        builder.init()
        stages.add(builder.build())
    }

    fun parallelStage(init: ParallelStageBuilder.() -> Unit) {
        val builder = ParallelStageBuilder("stage${stages.size}")
        builder.init()
        stages.add(builder.build())
    }

    fun build() = Benchmark(
        name = name,
        stages = stages,
        dictionaries = dictionaries,
        variables = variables,
    )
}

class SequentialStageBuilder(var name: String) {
    var sync: Boolean = false
    var dictionaries: Dictionaries = emptyMap()
    var variables: Template = Template.EMPTY
    private val workloads = mutableListOf<RateWorkload>()

    fun rateWorkload(
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
        executor: Executor,
        name: String? = null,
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