package uk.dioxic.helios.generate.extensions

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.bson.BsonDocument
import org.bson.BsonValue

fun Map<String, Any?>.flatten(separator: Char = '.') =
    mutableMapOf<String, Any?>().also {
        flatten(it, this, separator)
    }.toMap()

fun JsonObject.flatten(separator: Char = '.') =
    mutableMapOf<String, JsonElement>().also {
        flatten(it, this, separator)
    }.toMap()

fun BsonDocument.flatten(separator: Char = '.') =
    mutableMapOf<String, BsonValue>().also {
        flatten(it, this, separator)
    }.toMap()


// TODO put complex objects into the map (maps + lists)
@Suppress("UNCHECKED_CAST")
private fun <T> flatten(map: MutableMap<String, T>, value: T, separator: Char, key: String = "") {
    return when (value) {
        is Map<*, *> -> {
            value.filterValues { it != null }.forEach { (k, v) ->
                val newKey = getKey(key, separator, k.toString())
                flatten(map, v as T, separator, newKey)
            }
        }

        is Iterable<*> -> {
            value.filterNotNull().forEachIndexed { i, v ->
                val newKey = getKey(key, separator, i.toString())
                flatten(map, v as T, separator, newKey)
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