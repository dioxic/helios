package uk.dioxic.helios.execute.model

import arrow.optics.optics
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import uk.dioxic.helios.execute.Stateful
import uk.dioxic.helios.generate.Template
import uk.dioxic.helios.generate.hydrateAndFlatten
import kotlin.time.Duration

@optics
@Serializable
sealed class Stage : Stateful {
    abstract val sync: Boolean
    abstract val workloads: List<Workload>
    abstract val timeout: Duration

    @Transient
    override val constants = lazy { constantsDefinition.hydrateAndFlatten() }

    companion object
}

@optics
@Serializable
@SerialName("sequential")
data class SequentialStage(
    override val name: String,
    override val sync: Boolean = false,
    @SerialName("constants") override val constantsDefinition: Template = Template.EMPTY,
    @SerialName("variables") override val variablesDefinition: Template = Template.EMPTY,
    override val workloads: List<RateWorkload>,
    override val timeout: Duration = Duration.INFINITE,
) : Stage() {
    companion object
}

@optics
@Serializable
@SerialName("parallel")
data class ParallelStage(
    override val name: String,
    override val sync: Boolean = false,
    override val workloads: List<Workload>,
    @SerialName("constants") override val constantsDefinition: Template = Template.EMPTY,
    @SerialName("variables") override val variablesDefinition: Template = Template.EMPTY,
    override val timeout: Duration = Duration.INFINITE,
    val weightedWorkloadRate: Rate = UnlimitedRate,
) : Stage() {
    companion object
}