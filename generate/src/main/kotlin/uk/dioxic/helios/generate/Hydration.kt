@file:Suppress("UNCHECKED_CAST")

package uk.dioxic.helios.generate

//TODO use context receiver
fun Map<String, *>.hydrate(): Map<String, *> = with (OperatorContext.EMPTY) {
    hydrate(this@hydrate) as Map<String, *>
}

context (OperatorContext)
fun hydrate(value: Any?): Any? =
    when (value) {
        is Map<*, *> -> value.mapValues { (_, v) -> hydrate(v) }
        is Iterable<*> -> value.map { hydrate(it) }
        is Wrapped<*> -> hydrate(value.invoke())
        else -> value
    }