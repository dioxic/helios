package uk.dioxic.helios.execute.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import uk.dioxic.helios.execute.Named
import uk.dioxic.helios.execute.Stateful
import uk.dioxic.helios.generate.Template

@Serializable
data class Benchmark(
    override val name: String,
    override val state: Template = Template.EMPTY,
    val stages: List<Stage>
) : Named, Stateful {

    @Transient
    override val hydratedState = State(state.hydrate())
}

