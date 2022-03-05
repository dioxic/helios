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
import uk.dioxic.mgenerate.wrap

internal class OperatorBuilderTest {

    data class ArrayArguments(
        val of: Any,
        val number: Number
    ) {
        fun toDocument() = Document(mapOf(
            "of" to of,
            "number" to number
        ))
    }

    data class TextArguments(
        val length: Number,
        val characterPool: String?
    ) {
        fun toDocument() = Document().apply {
            put("length", length)
            characterPool?.let {
                put("characterPool", it)
            }
        }
    }

    @Nested
    inner class Array {
        private val operatorData = listOf(
            ArrayArguments("ABC", 6.0),
            ArrayArguments(111, 10L),
            ArrayArguments(ObjectId(), 4),
            ArrayArguments(mapOf("name" to "Bob", "age" to 25), 5),
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
        fun typedInputs() = operatorData
            .map { args ->
                DynamicTest.dynamicTest("Array operator build with $args is successful") {
                    assertThat {
                        ArrayBuilder()
                            .of(args.of)
                            .number(args.number)
                            .build()
                    }.isSuccess()
                        .transform { it() }
                        .check(args)
                }
            }

        @TestFactory
        @DisplayName("Functional inputs")
        fun functionalInputs() = operatorData
            .map { args ->
                DynamicTest.dynamicTest("Array operator build with $args is successful") {
                    assertThat {
                        ArrayBuilder()
                            .of { args.of }
                            .number(args.number.wrap())
                            .build()
                    }.isSuccess()
                        .transform { it() }
                        .check(args)
                }
            }

        @TestFactory
        @DisplayName("Map input")
        fun mapInput() = operatorData
            .map { args ->
                DynamicTest.dynamicTest("Array operator build with $args is successful") {
                    assertThat {
                        ArrayBuilder().from(args.toDocument())
                    }.isSuccess()
                        .transform { it() }
                        .check(args)
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

        private fun Assert<List<Any>>.check(args: ArrayArguments) = all {
            hasSize(args.number.toInt())
        }
    }

    @Nested
    inner class Text {
        private val validArgs = listOf(
            TextArguments(5, "ABC"),
            TextArguments(10L, "XZY"),
            TextArguments(16.5, "KLG"),
            TextArguments(5, null),
        )

        private val invalidArgs = listOf(
            mapOf("length" to "invalid"),
            mapOf("length" to null),
            mapOf("length" to 5, "characterPool" to 999),
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
                        .transform { it() }
                        .check(args)
                }
            }

        @TestFactory
        @DisplayName("Functional inputs")
        fun functionalInputs() = validArgs
            .map { args ->
                DynamicTest.dynamicTest("Text operator build with $args is successful") {
                    assertThat {
                        val builder = TextBuilder()
                        args.characterPool?.let {
                            builder.characterPool { it }
                        }
                        builder.length(args.length.wrap())
                        builder.build()
                    }.isSuccess()
                        .transform { it() }
                        .check(args)
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
                        .transform { it() }
                        .check(args)
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

        private fun Assert<String>.check(args: TextArguments) = all {
            hasLength(args.length.toInt())
            args.characterPool?.let {
                isSubsetOf(it)
            }
        }
    }
}



