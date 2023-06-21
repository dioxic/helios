package uk.dioxic.mgenerate.execute

import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import uk.dioxic.mgenerate.execute.results.MessageResult
import kotlin.time.Duration.Companion.milliseconds

class FlowTest : FunSpec({

    test("chunked flow") {
        runBlocking {
            flow {
                repeat(100) {
                    val timedResult = measureTimedResult(defaultExecutionContext) {
                        MessageResult("[$it] hello world!")
                    }

                    emit(timedResult)
                    delay(20.milliseconds)
                }
            }.summarize(500.milliseconds)
                .flowOn(Dispatchers.Default)
                .collect {
                    println(it)
                }
        }
    }

})