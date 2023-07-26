package uk.dioxic.helios.execute.model

import arrow.optics.optics
import kotlinx.serialization.Serializable
import uk.dioxic.helios.execute.serialization.WorkloadSerializer
import uk.dioxic.helios.generate.Named
import uk.dioxic.helios.generate.Template
import kotlin.time.Duration

@Serializable(WorkloadSerializer::class)
@optics sealed interface Workload : Named {
    val executor: Executor
    val count: Long
    val variables: Template
    val disable: Boolean

    companion object
}

@Serializable
@optics data class RateWorkload(
    override val name: String,
    override val variables: Template = Template.EMPTY,
    override val executor: Executor,
    override val count: Long = 1,
    override val disable: Boolean = false,
    val rate: Rate = UnlimitedRate,
    val startDelay: Duration = Duration.ZERO,
) : Workload {
    companion object
}

@Serializable
@optics data class WeightedWorkload(
    override val name: String,
    override val variables: Template = Template.EMPTY,
    override val executor: Executor,
    override val count: Long = 1,
    override val disable: Boolean = false,
    val weight: Int,
) : Workload {
    companion object
}