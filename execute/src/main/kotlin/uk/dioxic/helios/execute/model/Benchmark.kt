package uk.dioxic.helios.execute.model

import arrow.optics.optics
import kotlinx.serialization.Serializable
import uk.dioxic.helios.generate.Named
import uk.dioxic.helios.generate.Template

@Serializable
@optics
data class Benchmark(
    override val name: String,
    val variables: Template = Template.EMPTY,
    val dictionaries: Dictionaries = emptyMap(),
    val stages: List<Stage>
): Named {

    companion object {
        val EMPTY = Benchmark(
            name = "empty",
            dictionaries = emptyMap(),
            stages = emptyList(),
        )
    }
}

