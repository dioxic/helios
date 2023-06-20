package uk.dioxic.mgenerate.worker.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import uk.dioxic.mgenerate.Template
import uk.dioxic.mgenerate.worker.Named
import uk.dioxic.mgenerate.worker.Stateful

@Serializable
data class Benchmark(
    override val name: String,
    override val state: Template = Template.EMPTY,
    val stages: List<Stage>
) : Named, Stateful {

    @Transient
    override val hydratedState = State(state.hydrate())
}

