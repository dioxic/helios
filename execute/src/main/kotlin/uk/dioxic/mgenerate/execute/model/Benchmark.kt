package uk.dioxic.mgenerate.execute.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import uk.dioxic.mgenerate.execute.Named
import uk.dioxic.mgenerate.execute.Stateful
import uk.dioxic.mgenerate.template.Template

@Serializable
data class Benchmark(
    override val name: String,
    override val state: Template = Template.EMPTY,
    val stages: List<Stage>
) : Named, Stateful {

    @Transient
    override val hydratedState = State(state.hydrate())
}

