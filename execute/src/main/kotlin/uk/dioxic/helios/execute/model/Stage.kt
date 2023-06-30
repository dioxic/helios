package uk.dioxic.helios.execute.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import uk.dioxic.helios.execute.Stateful
import uk.dioxic.helios.generate.Template
import uk.dioxic.helios.generate.hydrateAndFlatten
import kotlin.time.Duration

@Serializable
sealed class Stage : Stateful {
    abstract val workloads: List<Workload>
    abstract val timeout: Duration

    @Transient
    override val constants = lazy { constantsDefinition.hydrateAndFlatten(this) }

}

@Serializable
@SerialName("sequential")
data class SequentialStage(
    override val name: String,
    @SerialName("constants") override val constantsDefinition: Template  = Template.EMPTY,
    @SerialName("variables") override val variablesDefinition: Template  = Template.EMPTY,
    override val workloads: List<RateWorkload>,
    override val timeout: Duration = Duration.INFINITE,
) : Stage()

@Serializable
@SerialName("parallel")
data class ParallelStage(
    override val name: String,
    override val workloads: List<Workload>,
    @SerialName("constants") override val constantsDefinition: Template  = Template.EMPTY,
    @SerialName("variables") override val variablesDefinition: Template  = Template.EMPTY,
    override val timeout: Duration = Duration.INFINITE,
    val weightedWorkloadRate: Rate = UnlimitedRate,
) : Stage()