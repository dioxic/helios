package uk.dioxic.mgenerate.worker.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import uk.dioxic.mgenerate.Template
import uk.dioxic.mgenerate.serialization.WorkloadSerializer
import uk.dioxic.mgenerate.worker.Named
import uk.dioxic.mgenerate.worker.Stateful
import kotlin.time.Duration

@Serializable(WorkloadSerializer::class)
sealed class Workload : Named, Stateful {
    abstract val executor: Executor
    abstract val count: Long

    abstract fun createContext(benchmark: Benchmark,stage: Stage): ExecutionContext
}

@Serializable
data class RateWorkload(
    override val name: String,
    override val state: Template = Template.EMPTY,
    override val executor: Executor,
    override val count: Long = 1,
    val rate: Rate = UnlimitedRate,
    val startDelay: Duration = Duration.ZERO,
) : Workload() {

    @Transient
    override val hydratedState = State(state.hydrate())

    override fun createContext(benchmark: Benchmark, stage: Stage) = ExecutionContext(
        workload = this,
        executor = executor,
        state = benchmark.hydratedState + stage.hydratedState + hydratedState,
        rate = rate,
    )

}

@Serializable
data class WeightedWorkload(
    override val name: String,
    override val state: Template = Template.EMPTY,
    override val executor: Executor,
    override val count: Long = 1,
    val weight: Int,
) : Workload() {

    @Transient
    override val hydratedState = State(state.hydrate())

    override fun createContext(benchmark: Benchmark, stage: Stage): ExecutionContext {
        require(stage is ParallelStage) {
            "Unexpected stage type of ${stage::class}"
        }
        return ExecutionContext(
            workload = this,
            executor = executor,
            state = benchmark.hydratedState + stage.hydratedState + hydratedState,
            rate = stage.weightedWorkloadRate,
        )
    }

}

