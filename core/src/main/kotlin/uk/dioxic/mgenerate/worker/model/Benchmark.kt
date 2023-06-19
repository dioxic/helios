package uk.dioxic.mgenerate.worker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import uk.dioxic.mgenerate.Template
import uk.dioxic.mgenerate.worker.Named
import kotlin.time.Duration

@Serializable
data class Benchmark(
    override val name: String,
    val state: Template = Template.EMPTY,
    val stages: List<Stage>
) : Named

@Serializable
sealed interface Stage : Named {
    val state: Template
    val workloads: List<Workload>
}

@Serializable
@SerialName("sequential")
data class SequentialStage(
    override val name: String,
    override val state: Template = Template.EMPTY,
    override val workloads: List<Workload>,
) : Stage

@Serializable
@SerialName("parallel")
data class ParallelStage(
    override val name: String,
    override val workloads: List<Workload>,
    override val state: Template = Template.EMPTY,
    val timeout: Duration = Duration.INFINITE,
) : Stage

