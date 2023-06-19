package uk.dioxic.mgenerate.worker.model

import kotlinx.serialization.Serializable
import uk.dioxic.mgenerate.Template
import uk.dioxic.mgenerate.serialization.WorkloadSerializer
import uk.dioxic.mgenerate.worker.Named
import kotlin.time.Duration

//@Serializable(WorkloadSerializer::class)
//sealed interface Workload: Named {
//    val state: Template
//    val executor: Executor<*>
//    val count: Long
//}

@Serializable(WorkloadSerializer::class)
sealed class Workload : Named {
    abstract val state: Template
    abstract val executor: Executor<*>
    abstract val count: Long
}

@Serializable
data class RateWorkload(
    override val name: String,
    override val state: Template = Template.EMPTY,
    override val executor: Executor<*>,
    override val count: Long = 1,
    val rate: Rate = UnlimitedRate,
    val startDelay: Duration = Duration.ZERO,
) : Workload()

@Serializable
data class WeightedWorkload(
    override val name: String,
    override val state: Template = Template.EMPTY,
    override val executor: Executor<*>,
    override val count: Long = 1,
    val weight: Int,
) : Workload()

