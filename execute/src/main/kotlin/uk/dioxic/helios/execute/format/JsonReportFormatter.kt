package uk.dioxic.helios.execute.format

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.bson.Bson
import kotlinx.serialization.encodeToString
import uk.dioxic.helios.execute.FrameworkMessage
import uk.dioxic.helios.execute.ProgressMessage
import uk.dioxic.helios.execute.StageCompleteMessage
import uk.dioxic.helios.execute.StageStartMessage
import uk.dioxic.helios.execute.results.SummarizedResultsBatch
import uk.dioxic.helios.execute.results.TimedExceptionResult
import uk.dioxic.helios.execute.results.TimedExecutionResult

data object JsonReportFormatter : ReportFormatter() {

    val bson = Bson {
        encodeDefaults = false
    }

    override fun format(results: Flow<FrameworkMessage>): Flow<String> = flow {
        var currentStageName = ""

        results.collect { msg ->
            when (msg) {
                is StageStartMessage -> {
                    currentStageName = msg.stage.name
                }

                is ProgressMessage -> {
                    when (val fRes = msg.result) {
                        is SummarizedResultsBatch -> {
                            fRes.results.forEach { result ->
                                emit(bson.encodeToString(result.toOutputResult(currentStageName)))
                            }
                        }

                        is TimedExecutionResult -> {
                            emit(bson.encodeToString(fRes.toOutputResult(currentStageName)))
                        }

                        is TimedExceptionResult -> {
                            emit(bson.encodeToString(fRes.toOutputResult(currentStageName)))
                        }
                    }
                }

                is StageCompleteMessage -> {}
            }
        }
    }
}