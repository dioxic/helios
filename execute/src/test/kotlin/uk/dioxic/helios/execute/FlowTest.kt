package uk.dioxic.helios.execute

import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import uk.dioxic.helios.execute.results.MessageResult
import uk.dioxic.helios.execute.test.IS_NOT_GH_ACTION
import kotlin.time.Duration.Companion.milliseconds

class FlowTest : FunSpec({

    xtest("chunked flow").config(enabled = IS_NOT_GH_ACTION) {
        flow {
            repeat(100) {
                val timedResult = defaultExecutionContext.measureTimedResult {
                    MessageResult(mapOf("hello" to "world"))
                }

                emit(timedResult)
                delay(20.milliseconds)
            }
        }.chunked(500.milliseconds)
            .flowOn(Dispatchers.Default)
            .collect {
                println(it)
            }
    }

})