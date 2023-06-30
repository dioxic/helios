package uk.dioxic.helios.execute.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import uk.dioxic.helios.execute.Stateful
import uk.dioxic.helios.execute.serialization.WorkloadSerializer
import uk.dioxic.helios.generate.Named
import uk.dioxic.helios.generate.Template
import uk.dioxic.helios.generate.hydrateAndFlatten
import kotlin.time.Duration

@Serializable(WorkloadSerializer::class)
sealed class Workload : Named, Stateful {
    abstract val executor: Executor
    abstract val count: Long

    abstract fun createContext(benchmark: Benchmark, stage: Stage): ExecutionContext

    @Transient
    override val constants = lazy { constantsDefinition.hydrateAndFlatten(this) }

    @Transient
    override val variables = lazy { variablesDefinition.hydrateAndFlatten(this) }
}

@Serializable
data class RateWorkload(
    override val name: String,
    @SerialName("constants") override val constantsDefinition: Template  = Template.EMPTY,
    @SerialName("variables") override val variablesDefinition: Template  = Template.EMPTY,
    override val executor: Executor,
    override val count: Long = 1,
    val rate: Rate = UnlimitedRate,
    val startDelay: Duration = Duration.ZERO,
) : Workload() {

    override fun createContext(benchmark: Benchmark, stage: Stage) = ExecutionContext(
        workload = this,
        executor = executor,
        constants = lazy { benchmark.constants.value + stage.constants.value + constants.value },
        rate = rate,
    )

}

@Serializable
data class WeightedWorkload(
    override val name: String,
    @SerialName("constants") override val constantsDefinition: Template  = Template.EMPTY,
    @SerialName("variables") override val variablesDefinition: Template  = Template.EMPTY,
    override val executor: Executor,
    override val count: Long = 1,
    val weight: Int,
) : Workload() {

    override fun createContext(benchmark: Benchmark, stage: Stage): ExecutionContext {
        require(stage is ParallelStage) {
            "Unexpected stage type of ${stage::class}"
        }
        return ExecutionContext(
            workload = this,
            executor = executor,
            constants = lazy { benchmark.constants.value + stage.constants.value + constants.value },
            rate = stage.weightedWorkloadRate,
        )
    }

}

