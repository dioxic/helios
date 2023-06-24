package uk.dioxic.mgenerate.execute

import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import uk.dioxic.mgenerate.execute.results.MessageResult
import uk.dioxic.mgenerate.execute.test.IS_NOT_GH_ACTION
import kotlin.time.Duration.Companion.milliseconds

class FlowTest : FunSpec({

    test("chunked flow").config(enabled = IS_NOT_GH_ACTION) {
        runBlocking {
            flow {
                repeat(100) {
                    val timedResult = defaultExecutionContext.measureTimedResult {
                        MessageResult("[$it] hello world!")
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
    }

})