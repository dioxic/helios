package uk.dioxic.helios.execute.model

import arrow.optics.optics
import uk.dioxic.helios.generate.OperatorContext

@optics
data class StateContext(
    override val count: Long = -1,
    override val dictionaries: Map<String, Any?> = emptyMap(),
    override val variables: Map<String, Any?> = emptyMap(),
    val dictionaryList: List<HydratedDictionary> = emptyList(),
//    val benchmarkDictionaries: Map<String, HydratedDictionary>,
//    val stateDictionaries: Map<String, HydratedDictionary>
) : OperatorContext {
    companion object
}
