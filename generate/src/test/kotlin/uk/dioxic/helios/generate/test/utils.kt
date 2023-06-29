package uk.dioxic.helios.generate.test

import uk.dioxic.helios.generate.OperatorContext

fun readResource(filename: String) =
    object {}.javaClass.getResourceAsStream(filename)?.bufferedReader()?.readText()
        ?: error("$filename resource not found!")

val IS_GH_ACTION = when (System.getenv("IS_GH_ACTION")) {
    "true" -> true
    else -> false
}

val IS_NOT_GH_ACTION = !IS_GH_ACTION

fun <R> withEmptyContext(block: OperatorContext.() -> R) = with(OperatorContext.EMPTY, block)