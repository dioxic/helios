package uk.dioxic.helios.generate.test

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import uk.dioxic.helios.generate.OperatorContext
import uk.dioxic.helios.generate.Template
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

private val json = Json { prettyPrint = true }

fun printJson(template: Template) {
    println(json.encodeToString(template))
}

fun printJson(jsonObject: JsonObject) {
    println(json.encodeToString(jsonObject))
}

//private fun hydrateAndPrint(map: Map<String, Any?>): Map<String, Any?> {
//    val res = with(OperatorContext.EMPTY) { map.hydrate() }
//    println(res)
//    return res
//}

fun Template.hydrateAndPrint(): Map<String, *> {
    val res = with(OperatorContext.EMPTY) { this@hydrateAndPrint.hydrate() }
    println(res)
    return res
}