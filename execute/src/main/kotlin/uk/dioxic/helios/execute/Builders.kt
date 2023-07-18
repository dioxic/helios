package uk.dioxic.helios.execute

import uk.dioxic.helios.execute.model.*
import uk.dioxic.helios.generate.Template
import kotlin.time.Duration

fun buildBenchmark(
    name: String = "benchmark",
    constants: Template = Template.EMPTY,
    variables: Template = Template.EMPTY,
    init: BenchmarkBuilder.() -> Unit
): Benchmark {
    val builder = BenchmarkBuilder(name, constants, variables)
    builder.init()
    return builder.build()
}

fun buildSequentialStage(
    name: String = "sequential",
    sync: Boolean = false,
    constants: Template = Template.EMPTY,
    variables: Template = Template.EMPTY,
    init: SequentialStageBuilder.() -> Unit
): SequentialStage {
    val builder = SequentialStageBuilder(
        name = name,
        sync = sync,
        constants = constants,
        variables = variables,
    )
    builder.init()
    return builder.build()
}

fun buildParallelStage(
    name: String = "parallel",
    sync: Boolean = false,
    constants: Template = Template.EMPTY,
    variables: Template = Template.EMPTY,
    timeout: Duration = Duration.INFINITE,
    init: ParallelStageBuilder.() -> Unit
): ParallelStage {
    val builder = ParallelStageBuilder(
        name = name,
        sync = sync,
        constants = constants,
        variables = variables,
        timeout = timeout
    )
    builder.init()
    return builder.build()
}

class BenchmarkBuilder(
    private val name: String,
    private val constants: Template,
    private val variables: Template,
) {
    private val stages = mutableListOf<Stage>()

    fun sequentialStage(
        name: String? = null,
        sync: Boolean = false,
        constants: Template = Template.EMPTY,
        variables: Template = Template.EMPTY,
        init: SequentialStageBuilder.() -> Unit
    ) {
        val builder = SequentialStageBuilder(
            name = name ?: "stage${stages.size}",
            sync = sync,
            constants = constants,
            variables = variables,
        )
        builder.init()
        stages.add(builder.build())
    }

    fun parallelStage(
        name: String? = null,
        sync: Boolean = false,
        constants: Template = Template.EMPTY,
        variables: Template = Template.EMPTY,
        timeout: Duration = Duration.INFINITE,
        init: ParallelStageBuilder.() -> Unit
    ) {
        val builder = ParallelStageBuilder(
            name = name ?: "stage${stages.size}",
            sync = sync,
            timeout = timeout,
            constants = constants,
            variables = variables,
        )
        builder.init()
        stages.add(builder.build())
    }

    fun build() = Benchmark(
        name = name,
        stages = stages,
        constantsDefinition = constants,
        variablesDefinition = variables,
    )
}

class SequentialStageBuilder(
    private val name: String,
    private val sync: Boolean,
    private val constants: Template,
    private val variables: Template,
) {
    private val workloads = mutableListOf<RateWorkload>()

    fun rateWorkload(
        name: String? = null,
        executor: Executor,
        count: Long = 1,
        constants: Template = Template.EMPTY,
        variables: Template = Template.EMPTY,
        rate: Rate = UnlimitedRate,
    ) {
        workloads.add(
            RateWorkload(
                name = name ?: "workload${workloads.size}",
                count = count,
                rate = rate,
                constantsDefinition = constants,
                variablesDefinition = variables,
                executor = executor
            )
        )
    }

    fun build() = SequentialStage(
        name = name,
        sync = sync,
        workloads = workloads,
        constantsDefinition = constants,
        variablesDefinition = variables,
    )
}

class ParallelStageBuilder(
    private val name: String,
    private val sync: Boolean,
    private val timeout: Duration,
    private val constants: Template,
    private val variables: Template,
) {
    private val workloads = mutableListOf<Workload>()

    fun rateWorkload(
        name: String? = null,
        executor: Executor,
        count: Long = 1,
        constants: Template = Template.EMPTY,
        variables: Template = Template.EMPTY,
        rate: Rate = UnlimitedRate,
    ) {
        workloads.add(
            RateWorkload(
                name = name ?: "workload${workloads.size}",
                count = count,
                rate = rate,
                constantsDefinition = constants,
                variablesDefinition = variables,
                executor = executor
            )
        )
    }

    fun weightedWorkload(
        executor: Executor,
        name: String? = null,
        weight: Int = 1,
        constants: Template = Template.EMPTY,
        variables: Template = Template.EMPTY,
        count: Long = 1,
    ) {
        workloads.add(
            WeightedWorkload(
                name = name ?: "workload${workloads.size}",
                count = count,
                weight = weight,
                constantsDefinition = constants,
                variablesDefinition = variables,
                executor = executor
            )
        )
    }

    fun build() = ParallelStage(
        name = name,
        sync = sync,
        timeout = timeout,
        workloads = workloads,
        constantsDefinition = constants,
        variablesDefinition = variables,
    )
}