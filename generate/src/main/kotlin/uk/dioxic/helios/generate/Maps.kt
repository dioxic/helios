package uk.dioxic.helios.generate

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.bson.BsonDocument
import org.bson.BsonValue

fun Map<String, Any?>.flatten(separator: Char = '.', leafOnly: Boolean = false) =
    mutableMapOf<String, Any?>().also {
        flatten(it, this, separator, leafOnly)
    }.toMap()

fun JsonObject.flatten(separator: Char = '.', leafOnly: Boolean = false) =
    mutableMapOf<String, JsonElement>().also {
        flatten(it, this, separator, leafOnly)
    }.toMap()

fun BsonDocument.flatten(separator: Char = '.', leafOnly: Boolean = false) =
    mutableMapOf<String, BsonValue>().also {
        flatten(it, this, separator, leafOnly)
    }.toMap()

fun <V> templateOf(pair: Pair<String, V>): Template = Template(mapOf(pair))

fun <V> templateOf(vararg pairs: Pair<String, V>): Template =
    Template(mapOf(*pairs))

@Suppress("UNCHECKED_CAST")
private fun <T> flatten(
    map: MutableMap<String, T>,
    value: T,
    separator: Char,
    leafOnly: Boolean,
    key: String = ""
) {
    when (value) {
        is Map<*, *> -> {
            value.filterValues { it != null }.forEach { (k, v) ->
                val newKey = getKey(key, separator, k.toString())
                flatten(map, v as T, separator, leafOnly, newKey)
            }
            if (!leafOnly && key.isNotEmpty()) {
                map[key] = value
            }
        }

        is Iterable<*> -> {
            value.filterNotNull().forEachIndexed { i, v ->
                val newKey = getKey(key, separator, i.toString())
                flatten(map, v as T, separator, leafOnly, newKey)
            }
            if (!leafOnly) {
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