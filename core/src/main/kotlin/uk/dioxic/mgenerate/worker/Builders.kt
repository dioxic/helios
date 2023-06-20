package uk.dioxic.mgenerate.worker

import uk.dioxic.mgenerate.worker.model.*
import kotlin.time.Duration

fun buildBenchmark(name: String = "benchmark", init: BenchmarkBuilder.() -> Unit): Benchmark {
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

class SequentialStageBuilder(private val name: String) {
    private val workloads = mutableListOf<RateWorkload>()

    fun rateWorkload(
        name: String? = null,
        executor: Executor,
        count: Long = 1,
        rate: Rate = UnlimitedRate,
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

    fun build() =
        SequentialStage(name = name, workloads = workloads)
}

class ParallelStageBuilder(
    private val name: String,
    private val timeout: Duration
) {
    private val workloads = mutableListOf<Workload>()

    fun rateWorkload(
        name: String? = null,
        executor: Executor,
        count: Long = 1,
        rate: Rate = UnlimitedRate,
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

    fun weightedWorkload(
        executor: Executor,
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

    fun build() =
        ParallelStage(name = name, timeout = timeout, workloads = workloads)
}