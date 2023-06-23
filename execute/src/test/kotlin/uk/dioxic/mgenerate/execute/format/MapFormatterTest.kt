package uk.dioxic.mgenerate.execute.format

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.should
import java.time.LocalDateTime

class MapFormatterTest : FunSpec({

    test("non-nested map is correct") {
        val map = mapOf(
            "name" to "Bob",
            "date" to LocalDateTime.now(),
            "n" to 33
        )

        map.flatten('.').should {
            println(it)
            it shouldHaveSize 3
            it shouldContainKey "name"
            it shouldContainKey "date"
            it shouldContainKey "n"
        }
    }

    test("nested map is correct") {
        val map = mapOf(
            "name" to "Bob",
            "nested" to mapOf(
                "date" to LocalDateTime.now(),
                "n" to 33
            )
        )

        map.flatten('.').should {
            println(it)
            it shouldHaveSize 3
            it shouldContainKey "name"
            it shouldContainKey "nested.date"
            it shouldContainKey "nested.n"
        }
    }

    test("nested array is correct") {
        val map = mapOf(
            "name" to "Bob",
            "nested" to listOf("date", "n")
        )

        map.flatten('.').should {
            println(it)
            it shouldHaveSize 3
            it shouldContainKey "name"
            it shouldContainKey "nested.0"
            it shouldContainKey "nested.1"
        }
    }

    test("complex nested object is correct") {
        val map = mapOf(
            "name" to "Bob",
            "nested" to mapOf(
                "array" to listOf(mapOf("date" to "myDate"), "n")
            )
        )

        map.flatten('.').should {
            println(it)
            it shouldHaveSize 3
            it shouldContainKey "name"
            it shouldContainKey "nested.array.0.date"
            it shouldContainKey "nested.array.1"
        }
    }

})