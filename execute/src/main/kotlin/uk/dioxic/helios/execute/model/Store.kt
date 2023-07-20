package uk.dioxic.helios.execute.model

import kotlinx.serialization.Serializable
import uk.dioxic.helios.execute.serialization.StoreSerializer

@Serializable(StoreSerializer::class)
sealed interface Store {
    val persist: Boolean

    companion object {
        val YES = BooleanStore(true)
        val NO = BooleanStore(false)
    }
}

data class BooleanStore(
    override val persist: Boolean
): Store

data class PathStore(
    val path: String
) : Store {
    override val persist = true
}