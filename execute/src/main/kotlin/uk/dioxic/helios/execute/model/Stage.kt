package uk.dioxic.helios.execute.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import uk.dioxic.helios.execute.Named
import uk.dioxic.helios.execute.Stateful
import uk.dioxic.helios.generate.Template
import kotlin.time.Duration

@Serializable
sealed class Stage : Named, Stateful {
    abstract val workloads: List<Workload>
    abstract val timeout: Duration
}

@Serializable
@SerialName("sequential")
data class SequentialStage(
    override val name: String,
    override val state: Template = Template.EMPTY,
    override val workloads: List<RateWorkload>,
    override val timeout: Duration = Duration.INFINITE,
) : Stage() {

    @Transient
    override val hydratedState: State = State(state.hydrate())
}

@Serializable
@SerialName("parallel")
data class ParallelStage(
    override val name: String,
    override val state: Template = Template.EMPTY,
    override val workloads: List<Workload>,
    override val timeout: Duration = Duration.INFINITE,
    val weightedWorkloadRate: Rate = UnlimitedRate
) : Stage() {

    @Transient
    override val hydratedState: State = State(state.hydrate())
}