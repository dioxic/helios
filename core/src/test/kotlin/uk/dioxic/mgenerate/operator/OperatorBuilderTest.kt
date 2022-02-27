package uk.dioxic.mgenerate.operator

import assertk.all
import assertk.assertThat
import assertk.assertions.hasLength
import assertk.assertions.isFailure
import assertk.assertions.isSuccess
import assertk.tableOf
import org.junit.jupiter.api.Disabled
import uk.dioxic.mgenerate.Text
import uk.dioxic.mgenerate.TextBuilder
import uk.dioxic.mgenerate.extensions.isSubsetOf
import uk.dioxic.mgenerate.instance
import kotlin.test.Test

internal class OperatorBuilderTest {

    @Test
    fun `Build with lambda types`() {
        tableOf("length", "pool")
            .row(1, "ABC")
            .row(10, "XYZ")
            .forAll { length, pool ->
                assertThat {
                    TextBuilder()
                        .length { length }
                        .characterPool { pool }
                        .build()
                }.isSuccess()
                    .transform { it() }
                    .all {
                        hasLength(length)
                        isSubsetOf(pool)
                    }
            }
    }

    @Test
    fun `Build with scalar types`() {
        tableOf("length", "pool")
            .row(1, "ABC")
            .row(10, "XYZ")
            .forAll { length, pool ->
                assertThat {
                    TextBuilder()
                        .length(length)
                        .characterPool(pool)
                        .build()
                }.isSuccess()
                    .transform { it() }
                    .all {
                        hasLength(length)
                        isSubsetOf(pool)
                    }
            }
    }

    @Test
    fun `Build with missing parameters`() {
        tableOf("length", "pool")
            .row(1, "ABC")
            .row(10, "XYZ")
            .forAll { _, pool ->
                assertThat {
                    TextBuilder()
                        .characterPool(pool)
                        .build()
                }.isFailure()
            }
    }

    @Test
    fun `Build with map`() {
        tableOf("length", "pool")
            .row(1, "ABC")
            .row(10, "XYZ")
            .forAll { length, pool ->
                assertThat {
                    TextBuilder().from(mapOf(
                        "length" to length,
                        "characterPool" to { pool }
                    ))
                }.isSuccess()
                    .transform { it() }
                    .all {
                        hasLength(length)
                        isSubsetOf(pool)
                    }
            }
    }

    @Disabled
    @Test
    fun `Map with incorrect types`() {
        assertThat {
            TextBuilder().from(
                mapOf(
                    "length" to 5,
                    "characterPool" to { 45L }
                )
            )
        }.isSuccess()
            .transform {
                it()
            }
    }
}


