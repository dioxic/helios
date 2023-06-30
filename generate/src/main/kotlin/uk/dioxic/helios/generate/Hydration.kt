@file:Suppress("UNCHECKED_CAST")

package uk.dioxic.helios.generate

import uk.dioxic.helios.generate.extensions.flatten

fun Template.hydrateAndFlatten(named: Named) =
    with(NamedContext(named)) {
        this@hydrateAndFlatten.hydrate().flatten()
    }

context (OperatorContext)
fun Map<String, *>.hydrate(): Map<String, *> =
    hydrate(this) as Map<String, *>

context (OperatorContext)
fun hydrate(value: Any?): Any? =
    when (value) {
        is Map<*, *> -> value.mapValues { (_, v) -> hydrate(v) }
        is Iterable<*> -> value.map { hydrate(it) }
        is Wrapped<*> -> hydrate(value.invoke())
        else -> value
    }