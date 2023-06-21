package uk.dioxic.mgenerate.execute.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import uk.dioxic.mgenerate.execute.Named
import uk.dioxic.mgenerate.execute.Stateful
import uk.dioxic.mgenerate.template.Template
import kotlin.time.Duration

@Serializable
sealed class Stage : Named, Stateful {
    abstract val workloads: List<Workload>
}

@Serializable
@SerialName("sequential")
data class SequentialStage(
    override val name: String,
    override val state: Template = Template.EMPTY,
    override val workloads: List<RateWorkload>,
) : Stage() {

    @Transient
    override val hydratedState: State = State(state.hydrate())
}

@Serializable
@SerialName("parallel")
data class ParallelStage(
    override val name: String,
    override val workloads: List<Workload>,
    override val state: Template = Template.EMPTY,
    val timeout: Duration = Duration.INFINITE,
    val weightedWorkloadRate: Rate = UnlimitedRate
) : Stage() {

    @Transient
    override val hydratedState: State = State(state.hydrate())
}