package uk.dioxic.mgenerate.worker.model

import kotlinx.serialization.Serializable
import uk.dioxic.mgenerate.Template
import uk.dioxic.mgenerate.worker.Named

@Serializable
data class Benchmark(
    override val name: String,
    val state: Template = Template.EMPTY,
    val stages: List<Stage>
) : Named

