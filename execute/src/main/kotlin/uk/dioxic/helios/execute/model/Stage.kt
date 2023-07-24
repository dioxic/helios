package uk.dioxic.helios.execute.model

import arrow.optics.optics
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import uk.dioxic.helios.generate.Named
import uk.dioxic.helios.generate.Template
import kotlin.time.Duration

@optics
@Serializable
sealed interface Stage : Named {
    val sync: Boolean
    val workloads: List<Workload>
    val timeout: Duration
    val variables: Template
    val dictionaries: Dictionaries

    companion object
}

@optics
@Serializable
@SerialName("sequential")
data class SequentialStage(
    override val name: String,
    override val sync: Boolean = false,
    override val workloads: List<RateWorkload>,
    override val timeout: Duration = Duration.INFINITE,
    override val variables: Template = Template.EMPTY,
    override val dictionaries: Dictionaries = emptyMap(),
) : Stage {
    companion object
}

@optics
@Serializable
@SerialName("parallel")
data class ParallelStage(
    override val name: String,
    override val sync: Boolean = false,
    override val workloads: List<Workload>,
    override val timeout: Duration = Duration.INFINITE,
    override val variables: Template = Template.EMPTY,
    override val dictionaries: Dictionaries = emptyMap(),
    val weightedWorkloadRate: Rate = UnlimitedRate,
) : Stage {
    companion object
}