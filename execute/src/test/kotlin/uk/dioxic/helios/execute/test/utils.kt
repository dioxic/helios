package uk.dioxic.helios.execute.test

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import uk.dioxic.helios.execute.FrameworkMessage
import uk.dioxic.helios.execute.ProgressMessage
import uk.dioxic.helios.execute.results.MessageResult
import uk.dioxic.helios.execute.results.TimedExecutionResult

fun readResource(filename: String) =
    object {}.javaClass.getResourceAsStream(filename)?.bufferedReader()?.readText()
        ?: error("$filename resource not found!")

val IS_GH_ACTION = when (System.getenv("IS_GH_ACTION")) {
    "true" -> true
    else -> false
}

val IS_NOT_GH_ACTION = !IS_GH_ACTION

fun Flow<FrameworkMessage>.mapMessageResults() =
    filterIsInstance<ProgressMessage>()
        .map { it.result }
        .filterIsInstance<TimedExecutionResult>()
        .map { it.value }
        .filterIsInstance<MessageResult>()
