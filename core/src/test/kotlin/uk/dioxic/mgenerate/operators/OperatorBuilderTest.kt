package uk.dioxic.mgenerate.operators

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.*
import org.bson.Document
import org.bson.types.ObjectId
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import uk.dioxic.mgenerate.exceptions.OperatorArgumentException
import uk.dioxic.mgenerate.extensions.isSubsetOf

internal class OperatorBuilderTest {

    @Nested
    @DisplayName("Array Operator")
    inner class ArrayTests {
        private val validArgs = listOf(
            ArrayArguments("ABC", 6.0),
            ArrayArguments(listOf("ABC", "XYZ"), 6.0),
            ArrayArguments(111, 10L),
            ArrayArguments(ObjectId(), 4),
            ArrayArguments(mapOf("name" to "Bob", "age" to 25), 5),
        )

        private val validFunctionArgs = validArgs
            .map(ArrayArguments::toFunctionArguments) + listOf(
            ArrayFunctionArguments(
                of = Text(length = { 5 }),
                number = RandomInt(min = { 0 }, max = { 20 })
            )
        )

        private val invalidArgs = listOf(
            mapOf("number" to 6),
            mapOf("number" to 6, "of" to null),
            mapOf("number" to null, "of" to null),
            mapOf("number" to "6"),
            mapOf(),
        )

        @TestFactory
        @DisplayName("Typed inputs")
        fun typedInputs() = validArgs
            .map { args ->
                DynamicTest.dynamicTest("Array operator build with $args is successful") {
                    assertThat {
                        ArrayBuilder()
                            .of(args.of)
                            .number(args.number)
                            .build()
                    }.isSuccess()
                        .checkOperator(args)
                }
            }

        @TestFactory
        @DisplayName("Functional inputs")
        fun functionalInputs() = validFunctionArgs
            .map { args ->
                DynamicTest.dynamicTest("Array operator build with $args is successful") {
                    assertThat {
                        ArrayBuilder()
                            .of(args.of)
                            .number(args.number)
                            .build()
                    }.isSuccess()
                        .checkOperator()
                }
            }

        @TestFactory
        @DisplayName("Map input")
        fun mapInput() = validArgs
            .map { args ->
                DynamicTest.dynamicTest("Array operator build with $args is successful") {
                    assertThat {
                        ArrayBuilder().from(args.toDocument())
                    }.isSuccess()
                        .checkOperator(args)
                }
            }

        @TestFactory
        @DisplayName("Invalid input")
        fun invalidMapInput() = invalidArgs
            .map { args ->
                DynamicTest.dynamicTest("Array operator build fails with $args") {
                    assertThat {
                        ArrayBuilder().from(args)
                    }
                        .isFailure()
                        .isInstanceOf(OperatorArgumentException::class)
                }
            }

        private fun Assert<Array>.checkOperator() =
            transform("Invoke operator") { it().also { println(it) } }
                .all {
                    each {
                        it.isNotNull()
                        it.isNotInstanceOf(Function0::class)
                    }
                }

        private fun Assert<Array>.checkOperator(args: ArrayArguments) =
            transform("Invoke operator") { it().also { println(it) } }
                .all {
                    each {
                        it.isNotNull()
                        it.isNotInstanceOf(Function0::class)
                    }
                    hasSize(args.number.toInt())
                }
    }

    @Nested
    @DisplayName("Text Operator")
    inner class TextTests {
        private val validArgs = listOf(
            TextArguments(5, "ABC"),
            TextArguments(10L, "XZY"),
            TextArguments(16.5, "KLG"),
            TextArguments(5, null),
        )

        private val validFunctionArgs = validArgs
            .map(TextArguments::toFunctionArguments) + listOf(
            TextFunctionArguments(
                length = RandomInt(min = { 0 }, max = { 20 })
            ),
            TextFunctionArguments(
                length = RandomInt(min = { 5 }, max = { 10 }),
                characterPool = { "ABCDEF" }
            )
        )
        private val invalidArgs = listOf(
            mapOf("length" to "invalid"),
            mapOf("length" to null),
            mapOf("length" to 5, "characterPool" to 999),
            mapOf("length" to { "ABC" }),
            mapOf(),
        )

        @TestFactory
        @DisplayName("Typed inputs")
        fun typedInputs() = validArgs
            .map { args ->
                DynamicTest.dynamicTest("Text operator build with $args is successful") {
                    assertThat {
                        val builder = TextBuilder()
                        args.characterPool?.let {
                            builder.characterPool(it)
                        }
                        builder.length(args.length)
                        builder.build()
                    }.isSuccess()
                        .checkOperator(args)
                }
            }

        @TestFactory
        @DisplayName("Functional inputs")
        fun functionalInputs() = validFunctionArgs
            .map { args ->
                DynamicTest.dynamicTest("Text operator build with $args is successful") {
                    assertThat {
                        val builder = TextBuilder()
                        args.characterPool?.let {
                            builder.characterPool(it)
                        }
                        builder.length(args.length)
                        builder.build()
                    }.isSuccess()
                        .checkOperator(args)
                }
            }

        @TestFactory
        @DisplayName("Map input")
        fun mapInput() = validArgs
            .map { args ->
                DynamicTest.dynamicTest("Text operator build with $args is successful") {
                    assertThat {
                        TextBuilder()
                            .from(args.toDocument())
                    }.isSuccess()
                        .checkOperator(args)
                }
            }

        @TestFactory
        @DisplayName("Invalid input")
        fun invalidMapInput() = invalidArgs
            .map { args ->
                DynamicTest.dynamicTest("Text operator build fails with $args") {
                    assertThat {
                        TextBuilder()
                            .from(args)
                    }.isFailure()
                        .isInstanceOf(OperatorArgumentException::class)
                }
            }

        private fun Assert<Text>.checkOperator(args: TextFunctionArguments) =
            transform("Invoke operator") { it().also { println(it) } }
                .all {
                    args.characterPool?.let {
                        isSubsetOf(it())
                    }
                }

        private fun Assert<Text>.checkOperator(args: TextArguments) =
            transform("Invoke operator") { it() }
                .all {
                    hasLength(args.length.toInt())
                    args.characterPool?.let {
                        isSubsetOf(it)
                    }
                }
    }

    data class ArrayArguments(
        val of: Any,
        val number: Number
    ) {
        fun toDocument() = Document(
            mapOf(
                "of" to of,
                "number" to number
            )
        )

        fun toFunctionArguments() =
            ArrayFunctionArguments({ of }, { number })
    }

    data class ArrayFunctionArguments(
        val of: () -> Any,
        val number: () -> Number
    ) {
        fun toDocument() = Document(
            mapOf(
                "of" to of,
                "number" to number
            )
        )
    }

    data class TextArguments(
        val length: Number,
        val characterPool: String? = null
    ) {
        fun toDocument() = Document().apply {
            put("length", length)
            characterPool?.let {
                put("characterPool", it)
            }
        }

        fun toFunctionArguments() =
            TextFunctionArguments({ length }, characterPool?.let { { it } })
    }

    data class TextFunctionArguments(
        val length: () -> Number,
        val characterPool: (() -> String)? = null
    ) {
        fun toDocument() = Document().apply {
            put("length", length)
            characterPool?.let {
                put("characterPool", it)
            }
        }
    }
}



