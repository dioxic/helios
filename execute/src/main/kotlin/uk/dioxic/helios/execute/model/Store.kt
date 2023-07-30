package uk.dioxic.helios.execute.model

import kotlinx.serialization.Serializable
import okio.Path
import okio.Path.Companion.toPath
import uk.dioxic.helios.execute.serialization.StoreSerializer

const val defaultStoreExtension = "json"

@Serializable(StoreSerializer::class)
sealed interface Store {
    val persist: Boolean

    fun getOkioPath(key: String): Path? =
        when(this) {
            is PathStore -> path.toPath()
            is BooleanStore -> {
                if (persist) {
                    "$key.$defaultStoreExtension".toPath()
                } else {
                    null
                }
            }
        }

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