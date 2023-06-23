package uk.dioxic.mgenerate.execute.format

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import uk.dioxic.mgenerate.execute.FrameworkMessage
import uk.dioxic.mgenerate.execute.ProgressMessage
import uk.dioxic.mgenerate.execute.StageCompleteMessage
import uk.dioxic.mgenerate.execute.StageStartMessage
import uk.dioxic.mgenerate.execute.results.SummarizedResultsBatch
import uk.dioxic.mgenerate.execute.results.TimedResult

object JsonReportFormatter : ReportFormatter() {

    val json = Json {
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
                                with(fRes.batchDuration) {
                                    emit(
                                        json.encodeToString(result.toOutputResult(currentStageName))
                                    )
                                }
                            }
                        }
                        is TimedResult -> {
                            emit(json.encodeToString(fRes.toOutputResult(currentStageName)))
                        }
                    }
                }
                is StageCompleteMessage -> {}
            }
        }
    }
}