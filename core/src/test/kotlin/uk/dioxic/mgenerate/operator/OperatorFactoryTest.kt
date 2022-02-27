package uk.dioxic.mgenerate.operator

import assertk.all
import assertk.assertThat
import assertk.assertions.hasLength
import assertk.assertions.isFailure
import assertk.assertions.isSuccess
import assertk.tableOf
import org.junit.jupiter.api.Disabled
import uk.dioxic.mgenerate.Text
import uk.dioxic.mgenerate.extensions.isSubsetOf
import uk.dioxic.mgenerate.instance
import kotlin.test.Test

@Disabled
internal class OperatorFactoryTest {

    @Test
    fun `Map with correct types`() {
        tableOf("length", "pool")
            .row(1, "ABC")
            .row(10, "XYZ")
            .forAll { length, pool ->
                val map = mapOf("length" to { length }, "characterPool" to { pool })
                assertThat { instance<Text>(map) }
                    .isSuccess()
                    .transform { it() }
                    .all {
                        hasLength(length)
                        isSubsetOf(pool)
                    }
            }
    }

    @Test
    fun `Map with incorrect types`() {
        tableOf("length", "pool")
            .row(1, 555)
            .row(10, 555)
            .forAll { length, pool ->
                val map = mapOf("length" to { length }, "characterPool" to { pool })
                assertThat { instance<Text>(map) }.isFailure()
            }
    }

    @Test
    fun `Single value with correct type`() {
        listOf(1,2,3,4,5).forEach { length ->
            assertThat { instance<Text> { length } }
                .isSuccess()
                .transform { it() }
                .all {
                    hasLength(length)
                }
        }
    }

    @Test
    fun `Single value with incorrect type`() {
        listOf("1", 2, 3).forEach { length ->
            assertThat { instance<Text> { length } }
                .isFailure()
        }
    }

}