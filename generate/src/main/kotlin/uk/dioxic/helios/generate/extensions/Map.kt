package uk.dioxic.helios.generate.extensions

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.bson.BsonDocument
import org.bson.BsonValue

fun Map<String, Any?>.flatten(separator: Char = '.', incBranches: Boolean = true) =
    mutableMapOf<String, Any?>().also {
        flatten(it, this, separator, incBranches)
    }.toMap()

fun JsonObject.flatten(separator: Char = '.', incBranches: Boolean = true) =
    mutableMapOf<String, JsonElement>().also {
        flatten(it, this, separator, incBranches)
    }.toMap()

fun BsonDocument.flatten(separator: Char = '.', incBranches: Boolean = true) =
    mutableMapOf<String, BsonValue>().also {
        flatten(it, this, separator, incBranches)
    }.toMap()


@Suppress("UNCHECKED_CAST")
private fun <T> flatten(
    map: MutableMap<String, T>,
    value: T,
    separator: Char,
    incBranches: Boolean = true,
    key: String = ""
) {
    when (value) {
        is Map<*, *> -> {
            value.filterValues { it != null }.forEach { (k, v) ->
                val newKey = getKey(key, separator, k.toString())
                flatten(map, v as T, separator, incBranches, newKey)
            }
            if (incBranches) {
                map[key] = value
            }
        }

        is Iterable<*> -> {
            value.filterNotNull().forEachIndexed { i, v ->
                val newKey = getKey(key, separator, i.toString())
                flatten(map, v as T, separator, incBranches, newKey)
            }
            if (incBranches) {
                map[key] = value
            }
        }

        else -> map[key] = value
    }
}

private fun getKey(prefix: String, separator: Char, suffix: String) =
    if (prefix.isEmpty()) {
        suffix
    } else {
        "$prefix$separator$suffix"
    }