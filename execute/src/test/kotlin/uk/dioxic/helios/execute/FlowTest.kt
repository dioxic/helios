package uk.dioxic.helios.execute

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
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

    test("groupBy") {
        val input = flowOf(
            "animal" to "Badger",
            "animal" to "Duck",
            "animal" to "Giraffi",
            "animal" to "Gorilla",
            "person" to "Bob",
            "person" to "Alice",
            "animal" to "Sheep",
            "animal" to "Turtle",
            "person" to "Luke",
            "person" to "Leila",
        )

        val expected = flowOf(
            "animal" to listOf("Badger", "Duck", "Giraffi", "Gorilla"),
            "person" to listOf("Bob", "Alice"),
            "animal" to listOf("Sheep", "Turtle"),
            "person" to listOf("Luke", "Leila")
        )

        assertSoftly {
            input.groupBy { key, list ->
                key to list
            }.onEach {
                println(it)
            }.zip(expected) { g, e ->
                g to e
            }.collect { (actual, expected) ->
                actual shouldBe expected
            }
        }

    }

})