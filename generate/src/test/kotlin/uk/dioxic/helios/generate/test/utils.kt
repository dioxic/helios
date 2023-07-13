package uk.dioxic.helios.generate.test

import uk.dioxic.helios.generate.OperatorContext
import uk.dioxic.helios.generate.hydrate

fun readResource(filename: String) =
    object {}.javaClass.getResourceAsStream(filename)?.bufferedReader()?.readText()
        ?: error("$filename resource not found!")

val IS_GH_ACTION = when (System.getenv("IS_GH_ACTION")) {
    "true" -> true
    else -> false
}

val IS_NOT_GH_ACTION = !IS_GH_ACTION

fun <R> withEmptyContext(block: OperatorContext.() -> R) = with(OperatorContext.EMPTY, block)

//private fun hydrateAndPrint(map: Map<String, Any?>): Map<String, Any?> {
//    val res = with(OperatorContext.EMPTY) { map.hydrate() }
//    println(res)
//    return res
//}

fun Map<String, *>.hydrateAndPrint(): Map<String, *> {
    val res = with(OperatorContext.EMPTY) { this@hydrateAndPrint.hydrate() }
    println(res)
    return res
}