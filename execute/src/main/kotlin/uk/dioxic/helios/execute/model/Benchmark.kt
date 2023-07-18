package uk.dioxic.helios.execute.model

import arrow.optics.optics
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import uk.dioxic.helios.execute.Stateful
import uk.dioxic.helios.generate.Template
import uk.dioxic.helios.generate.hydrateAndFlatten

@Serializable
@optics
data class Benchmark(
    override val name: String,
    @SerialName("constants") override val constantsDefinition: Template = Template.EMPTY,
    @SerialName("variables") override val variablesDefinition: Template = Template.EMPTY,
    val stages: List<Stage>
) : Stateful {

    @Transient
    override val constants = lazy { constantsDefinition.hydrateAndFlatten() }

    companion object {
        val EMPTY = Benchmark("empty", stages = emptyList())
    }
}

