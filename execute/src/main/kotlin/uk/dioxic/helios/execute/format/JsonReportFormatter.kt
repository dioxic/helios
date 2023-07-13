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
import uk.dioxic.helios.execute.results.TimedResult

object JsonReportFormatter : ReportFormatter() {

    val json = Bson {
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