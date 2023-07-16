package uk.dioxic.helios.execute.model

import arrow.optics.optics
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import uk.dioxic.helios.execute.Stateful
import uk.dioxic.helios.execute.serialization.WorkloadSerializer
import uk.dioxic.helios.generate.Template
import uk.dioxic.helios.generate.hydrateAndFlatten
import kotlin.time.Duration

@Serializable(WorkloadSerializer::class)
@optics sealed class Workload : Stateful {
    abstract val executor: Executor
    abstract val count: Long

    abstract fun createContext(benchmark: Benchmark, stage: Stage): ExecutionContext

    @Transient
    override val constants = lazy { constantsDefinition.hydrateAndFlatten(name) }

    companion object
}

@Serializable
@optics data class RateWorkload(
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
        constants = lazy(LazyThreadSafetyMode.NONE) {
            benchmark.constants.value + stage.constants.value + constants.value
        },
        variables = lazy(LazyThreadSafetyMode.NONE) {
            benchmark.variables + stage.variables + variables
        },
        rate = rate,
    )

    companion object
}

@Serializable
@optics data class WeightedWorkload(
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
            constants = lazy(LazyThreadSafetyMode.NONE) {
                benchmark.constants.value + stage.constants.value + constants.value
            },
            variables = lazy(LazyThreadSafetyMode.NONE) {
                benchmark.variables + stage.variables + variables
            },
            rate = stage.weightedWorkloadRate,
        )
    }

    companion object

}

