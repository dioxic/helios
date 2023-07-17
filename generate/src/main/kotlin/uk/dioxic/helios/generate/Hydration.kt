@file:Suppress("UNCHECKED_CAST")

package uk.dioxic.helios.generate

import uk.dioxic.helios.generate.operators.rootKey

fun Template.hydrateAndFlatten() =
    with(OperatorContext.EMPTY) {
        this@hydrateAndFlatten.hydrate().flatten()
    }

context (OperatorContext)
fun Map<String, *>.hydrate(): Map<String, *> =
    hydrate(this) as Map<String, *>

context (OperatorContext)
fun Template.hydrate(): Map<String, *> =
    hydrate(this) as Map<String, *>

context (OperatorContext)
fun hydrate(value: Any?): Any? =
    when (value) {
        is Map<*, *> -> {
            value[rootKey]?.let { root ->
                hydrate(root).also {
                    require(it is Map<*, *>) {
                        "Root operator must resolve to a map"
                    }
                }
            } ?: value.mapValues { (_, v) -> hydrate(v) }
        }

        is Iterable<*> -> value.map { hydrate(it) }
        is Wrapped<*> -> hydrate(value.invoke())
        else -> value
    }