@file:Suppress("UNCHECKED_CAST")

package uk.dioxic.helios.generate

import uk.dioxic.helios.generate.operators.RootOperator

fun Template.hydrateAndFlatten(named: Named) =
    with(NamedContext(named)) {
        this@hydrateAndFlatten.hydrate().flatten()
    }

context (OperatorContext)
fun Map<String, *>.hydrate(): Map<String, *> =
    hydrate(this) as Map<String, *>

private val rootOperatorKey = getOperatorKey<RootOperator>()

context (OperatorContext)
fun hydrate(value: Any?): Any? =
    when (value) {
        is Map<*, *> -> {
            value[rootOperatorKey]?.let { root ->
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