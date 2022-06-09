import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSuccess
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.dioxic.mgenerate.ksp.commons.indexOfFirstIgnoreQuoted
import uk.dioxic.mgenerate.ksp.commons.unquotedText

internal class UtilsTest {

    @Nested
    @DisplayName("Unquoted Text")
    inner class UnquotedTest {

        @Test
        fun `unquoted text`() {
            val input = """
                aaa"bbb"ccc
            """.trimIndent()
            println(input)

            assertThat { unquotedText(input) }
                .isSuccess()
                .isEqualTo("aaaccc")

        }

        @Test
        fun `unquoted text2`() {
            val input = """
                aaa"bbb\"bbb\"bbb"ccc
            """.trimIndent()

            println(input)
            println(
                input.split(Regex("""(?<!\\)""""))
            )

            assertThat { unquotedText(input) }
                .isSuccess()
                .isEqualTo("aaaccc")

        }
    }

    @Nested
    @DisplayName("IndexOfFirstIgnoreQuoted Extension")
    inner class IndexOfFirstIgnoreQuoted {

        @Test
        fun `no quoted text`() {
            val s = "abcd1233{}[]@"

            assertThat {
                s.indexOfFirstIgnoreQuoted { it == '3' }
            }
                .isSuccess()
                .isEqualTo(6)
        }

        @Test
        fun `no matches within quotes`() {
            val s = "abc\"d123\"3{}[]@"

            assertThat {
                s.indexOfFirstIgnoreQuoted { it == 'd' }
            }
                .isSuccess()
                .isEqualTo(-1)
        }

        @Test
        fun `matches outside of quotes`() {
            val s = "abc\"d123\"3{}[]@"

            assertThat {
                s.indexOfFirstIgnoreQuoted { it == '3' }
            }
                .isSuccess()
                .isEqualTo(9)
        }
    }


}