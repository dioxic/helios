package uk.dioxic.helios.generate

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import uk.dioxic.helios.generate.OperatorFactory.addOperator
import uk.dioxic.helios.generate.OperatorFactory.canHandle
import uk.dioxic.helios.generate.OperatorFactory.create
import uk.dioxic.helios.generate.exceptions.NoDefaultConfiguration
import uk.dioxic.helios.generate.exceptions.NoSingleValueParameter
import uk.dioxic.helios.generate.exceptions.OperatorConversionError
import uk.dioxic.helios.generate.exceptions.ParameterNotOptional
import uk.dioxic.helios.generate.fixture.*
import uk.dioxic.helios.generate.test.withEmptyContext

class OperatorFactoryTests : FunSpec({

    addOperator(OperatorWithSingleMandatoryArg::class)
    addOperator(OperatorWithMultiOptionalArg::class)
    addOperator(OperatorWithSingleOptionalArg::class)
    addOperator(OperatorWithMultiMandatoryArg::class)
    addOperator(KeyedOperatorWithNoArg::class)
    addOperator(KeyedOperatorWithSingleMandatoryArg::class)
    addOperator(KeyedOperatorWithMultiMandatoryArg::class)
    addOperator(KeyedOperatorWithSingleOptionalArg::class)
    addOperator(KeyedOperatorWithMultiOptionalArg::class)

    val expected = "halibut"
    val subKey = "subkey.badger"

    context("Normal Operator") {
        context("single mandatory arg") {
            val opKey = getOperatorKey<OperatorWithSingleMandatoryArg>()
            test("canHandle returns true") {
                canHandle(opKey).shouldBeTrue()
            }

            test("from value is successful") {
                create(opKey, expected).shouldBeRight().should {
                    it.shouldBeInstanceOf<OperatorWithSingleMandatoryArg>()
                    withEmptyContext {
                        it.arg.invoke() shouldBe expected
                    }
                }
            }
            test("from map is successful") {
                create(opKey, mapOf("arg" to expected)).shouldBeRight().should {
                    it.shouldBeInstanceOf<OperatorWithSingleMandatoryArg>()
                    withEmptyContext {
                        it.arg.invoke() shouldBe expected
                    }
                }
            }
            test("build fails") {
                create(opKey).shouldBeLeft().should {
                    it.shouldBeInstanceOf<NoDefaultConfiguration>()
                }
            }
        }

        context("multiple mandatory args") {
            val opKey = getOperatorKey<OperatorWithMultiMandatoryArg>()

            test("canHandle returns true") {
                canHandle(opKey).shouldBeTrue()
            }

            test("from value fails") {
                create(opKey, expected).shouldBeLeft().should {
                    it.shouldBeInstanceOf<NoSingleValueParameter>()
                }
            }
            test("from map is successful") {
                create(opKey, mapOf("arg" to expected, "arg2" to "other")).shouldBeRight().should {
                    it.shouldBeInstanceOf<OperatorWithMultiMandatoryArg>()
                    withEmptyContext {
                        it.arg.invoke() shouldBe expected
                    }
                }
            }
            test("build fails") {
                create(opKey).shouldBeLeft().should {
                    it.shouldBeInstanceOf<NoDefaultConfiguration>()
                }
            }
        }

        context("single optional arg") {
            val opKey = getOperatorKey<OperatorWithSingleOptionalArg>()

            test("canHandle returns true") {
                canHandle(opKey).shouldBeTrue()
            }

            test("from value is successful") {
                create(opKey, expected).shouldBeRight().should {
                    it.shouldBeInstanceOf<OperatorWithSingleOptionalArg>()
                    withEmptyContext {
                        it.arg.invoke() shouldBe expected
                    }
                }
            }
            test("from map is successful") {
                create(opKey, mapOf("arg" to expected)).shouldBeRight().should {
                    it.shouldBeInstanceOf<OperatorWithSingleOptionalArg>()
                    withEmptyContext {
                        it.arg.invoke() shouldBe expected
                    }
                }
            }
            test("build successful") {
                create(opKey).shouldBeRight().shouldBeInstanceOf<OperatorWithSingleOptionalArg>()
            }
        }

        context("mutiple optional args") {
            val opKey = getOperatorKey<OperatorWithMultiOptionalArg>()

            test("canHandle returns true") {
                canHandle(opKey).shouldBeTrue()
            }

            test("from value fails") {
                create(opKey, expected).shouldBeLeft().should {
                    it.shouldBeInstanceOf<NoSingleValueParameter>()
                }
            }
            test("from map is successful") {
                create(opKey, mapOf("arg" to expected, "arg2" to expected)).shouldBeRight().should {
                    it.shouldBeInstanceOf<OperatorWithMultiOptionalArg>()
                    withEmptyContext {
                        it.arg.invoke() shouldBe expected
                        it.arg2.invoke() shouldBe expected
                    }
                }
            }
            test("build successful") {
                create(opKey).shouldBeRight().shouldBeInstanceOf<OperatorWithMultiOptionalArg>()
            }
        }
    }

    context("Keyed Operator") {
        context("no args") {
            val opKey = getOperatorKey<KeyedOperatorWithNoArg>("subkey.badger")

            test("canHandle returns true") {
                canHandle(opKey).shouldBeTrue()
            }

            test("from value fails") {
                create(opKey, expected).shouldBeLeft().should {
                    it.shouldBeInstanceOf<NoSingleValueParameter>()
                }
            }
            test("from map is successful") {
                create(opKey, emptyMap<String, String>()).shouldBeRight().should {
                    it.shouldBeInstanceOf<KeyedOperatorWithNoArg>()
                    it.key shouldBe subKey
                }
            }
            test("build successful") {
                create(opKey).shouldBeRight().should {
                    it.shouldBeInstanceOf<KeyedOperatorWithNoArg>()
                    it.key shouldBe subKey
                }
            }
        }

        context("single mandatory arg") {
            val opKey = getOperatorKey<KeyedOperatorWithSingleMandatoryArg>(subKey)

            test("canHandle returns true") {
                canHandle(opKey).shouldBeTrue()
            }

            test("from value is successful") {
                create(opKey, expected).shouldBeRight().should {
                    it.shouldBeInstanceOf<KeyedOperatorWithSingleMandatoryArg>()
                    it.key shouldBe subKey
                    withEmptyContext {
                        it.arg.invoke() shouldBe expected
                    }
                }
            }
            test("from map is successful") {
                create(opKey, mapOf("arg" to expected)).shouldBeRight().should {
                    it.shouldBeInstanceOf<KeyedOperatorWithSingleMandatoryArg>()
                    it.key shouldBe subKey
                    withEmptyContext {
                        it.arg.invoke() shouldBe expected
                    }
                }
            }
            test("build fails") {
                create(opKey).shouldBeLeft().should {
                    it.shouldBeInstanceOf<OperatorConversionError>()
                    it.errors.forEach { e ->
                        e.shouldBeInstanceOf<ParameterNotOptional>()
                    }
                }
            }
        }

        context("multiple mandatory args") {
            val opKey = getOperatorKey<KeyedOperatorWithMultiMandatoryArg>(subKey)

            test("canHandle returns true") {
                canHandle(opKey).shouldBeTrue()
            }

            test("from value fails") {
                create(opKey, expected).shouldBeLeft().should {
                    it.shouldBeInstanceOf<NoSingleValueParameter>()
                }
            }
            test("from map is successful") {
                create(opKey, mapOf("arg" to expected, "arg2" to expected)).shouldBeRight().should {
                    it.shouldBeInstanceOf<KeyedOperatorWithMultiMandatoryArg>()
                    it.key shouldBe subKey
                    withEmptyContext {
                        it.arg.invoke() shouldBe expected
                        it.arg2.invoke() shouldBe expected
                    }
                }
            }
            test("build fails") {
                create(opKey).shouldBeLeft().should {
                    it.shouldBeInstanceOf<OperatorConversionError>()
                    it.errors.forEach { e ->
                        e.shouldBeInstanceOf<ParameterNotOptional>()
                    }
                }
            }
        }

        context("single optional arg") {
            val opKey = getOperatorKey<KeyedOperatorWithSingleOptionalArg>(subKey)

            test("canHandle returns true") {
                canHandle(opKey).shouldBeTrue()
            }

            test("from value is successful") {
                create(opKey, expected).shouldBeRight().should {
                    it.shouldBeInstanceOf<KeyedOperatorWithSingleOptionalArg>()
                    it.key shouldBe subKey
                    withEmptyContext { it.arg.invoke() shouldBe expected }
                }
            }
            test("from map is successful") {
                create(opKey, mapOf("arg" to expected)).shouldBeRight().should {
                    it.shouldBeInstanceOf<KeyedOperatorWithSingleOptionalArg>()
                    it.key shouldBe subKey
                    withEmptyContext { it.arg.invoke() shouldBe expected }
                }
            }
            test("build successful") {
                create(opKey).shouldBeRight().should {
                    it.shouldBeInstanceOf<KeyedOperatorWithSingleOptionalArg>()
                    it.key shouldBe subKey
                }
            }
        }

        context("mutiple optional args") {
            val opKey = getOperatorKey<KeyedOperatorWithMultiOptionalArg>(subKey)

            test("canHandle returns true") {
                canHandle(opKey).shouldBeTrue()
            }

            test("from value fails") {
                create(opKey, expected).shouldBeLeft().should {
                    it.shouldBeInstanceOf<NoSingleValueParameter>()
                }
            }
            test("from map is successful") {
                create(opKey, mapOf("arg" to expected, "arg2" to expected)).shouldBeRight().should {
                    it.shouldBeInstanceOf<KeyedOperatorWithMultiOptionalArg>()
                    it.key shouldBe subKey
                    withEmptyContext {
                        it.arg.invoke() shouldBe expected
                        it.arg2.invoke() shouldBe expected
                    }
                }
            }
            test("build successful") {
                create(opKey).shouldBeRight().should {
                    it.shouldBeInstanceOf<KeyedOperatorWithMultiOptionalArg>()
                    it.key shouldBe subKey
                }
            }
        }
    }


})