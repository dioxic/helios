package uk.dioxic.mgenerate.execute.format

fun Map<String, Any>.flatten(separator: Char) =
    mutableMapOf<String, Any>().also {
        flatten(it, this, separator)
    }

private fun flatten(map: MutableMap<String, Any>, value: Any, separator: Char, key: String = ""): Any {
    return when (value) {
        is Map<*, *> -> {
            value.filterValues { it != null }.forEach { (k, v) ->
                val newKey = getKey(key, separator, k.toString())
                flatten(map, v!!, separator, newKey)
            }
        }

        is Iterable<*> -> {
            value.filterNotNull().forEachIndexed { i, v ->
                val newKey = getKey(key, separator, i.toString())
                flatten(map, v, separator, newKey)
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