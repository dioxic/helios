package uk.dioxic.helios.generate

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import uk.dioxic.helios.generate.OperatorFactory.addOperator
import uk.dioxic.helios.generate.OperatorFactory.canHandle
import uk.dioxic.helios.generate.OperatorFactory.create
import uk.dioxic.helios.generate.fixture.*
import uk.dioxic.helios.generate.test.opKey
import uk.dioxic.helios.generate.test.withEmptyContext

class OperatorFactoryTest : FunSpec({

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
            val opKey = opKey<OperatorWithSingleMandatoryArg>()
            test("canHandle returns true") {
                canHandle(opKey).shouldBeTrue()
            }

            test("from value is successful") {
                create(opKey, expected).should {
                    it.shouldBeInstanceOf<OperatorWithSingleMandatoryArg>()
                    withEmptyContext {
                        it.arg.invoke() shouldBe expected
                    }
                }
            }
            test("from map is successful") {
                create(opKey, mapOf("arg" to expected)).should {
                    it.shouldBeInstanceOf<OperatorWithSingleMandatoryArg>()
                    withEmptyContext {
                        it.arg.invoke() shouldBe expected
                    }
                }
            }
            test("build fails") {
                shouldThrow<IllegalArgumentException> {
                    create(opKey)
                }
            }
        }

        context("multiple mandatory args") {
            val opKey = opKey<OperatorWithMultiMandatoryArg>()

            test("canHandle returns true") {
                canHandle(opKey).shouldBeTrue()
            }

            test("from value fails") {
                shouldThrow<IllegalArgumentException> {
                    create(opKey, expected)
                }
            }
            test("from map is successful") {
                create(opKey, mapOf("arg" to expected, "arg2" to "other")).should {
                    it.shouldBeInstanceOf<OperatorWithMultiMandatoryArg>()
                    withEmptyContext {
                        it.arg.invoke() shouldBe expected
                    }
                }
            }
            test("build fails") {
                shouldThrow<IllegalArgumentException> {
                    create(opKey)
                }
            }
        }

        context("single optional arg") {
            val opKey = opKey<OperatorWithSingleOptionalArg>()

            test("canHandle returns true") {
                canHandle(opKey).shouldBeTrue()
            }

            test("from value is successful") {
                create(opKey, expected).should {
                    it.shouldBeInstanceOf<OperatorWithSingleOptionalArg>()
                    withEmptyContext {
                        it.arg.invoke() shouldBe expected
                    }
                }
            }
            test("from map is successful") {
                create(opKey, mapOf("arg" to expected)).should {
                    it.shouldBeInstanceOf<OperatorWithSingleOptionalArg>()
                    withEmptyContext {
                        it.arg.invoke() shouldBe expected
                    }
                }
            }
            test("build successful") {
                create(opKey).shouldBeInstanceOf<OperatorWithSingleOptionalArg>()
            }
        }

        context("mutiple optional args") {
            val opKey = opKey<OperatorWithMultiOptionalArg>()

            test("canHandle returns true") {
                canHandle(opKey).shouldBeTrue()
            }

            test("from value fails") {
                shouldThrow<IllegalArgumentException> {
                    create(opKey, expected)
                }
            }
            test("from map is successful") {
                create(opKey, mapOf("arg" to expected, "arg2" to expected)).should {
                    it.shouldBeInstanceOf<OperatorWithMultiOptionalArg>()
                    withEmptyContext {
                        it.arg.invoke() shouldBe expected
                        it.arg2.invoke() shouldBe expected
                    }
                }
            }
            test("build successful") {
                create(opKey).shouldBeInstanceOf<OperatorWithMultiOptionalArg>()
            }
        }
    }

    context("Keyed Operator") {
        context("no args") {
            val opKey = opKey<KeyedOperatorWithNoArg>("subkey.badger")

            test("canHandle returns true") {
                canHandle(opKey).shouldBeTrue()
            }

            test("from value fails") {
                shouldThrow<IllegalArgumentException> {
                    create(opKey, expected)
                }
            }
            test("from map is successful") {
                create(opKey, emptyMap<String, String>()).should {
                    it.shouldBeInstanceOf<KeyedOperatorWithNoArg>()
                    it.key shouldBe subKey
                }
            }
            test("build successful") {
                create(opKey).should {
                    it.shouldBeInstanceOf<KeyedOperatorWithNoArg>()
                    it.key shouldBe subKey
                }
            }
        }

        context("single mandatory arg") {
            val opKey = opKey<KeyedOperatorWithSingleMandatoryArg>(subKey)

            test("canHandle returns true") {
                canHandle(opKey).shouldBeTrue()
            }

            test("from value fails") {
                create(opKey, expected).should {
                    it.shouldBeInstanceOf<KeyedOperatorWithSingleMandatoryArg>()
                    it.key shouldBe subKey
                    withEmptyContext {
                        it.arg.invoke() shouldBe expected
                    }
                }
            }
            test("from map is successful") {
                create(opKey, mapOf("arg" to expected)).should {
                    it.shouldBeInstanceOf<KeyedOperatorWithSingleMandatoryArg>()
                    it.key shouldBe subKey
                    withEmptyContext {
                        it.arg.invoke() shouldBe expected
                    }
                }
            }
            test("build successful") {
                shouldThrow<IllegalArgumentException> {
                    create(opKey)
                }
            }
        }

        context("multiple mandatory args") {
            val opKey = opKey<KeyedOperatorWithMultiMandatoryArg>(subKey)

            test("canHandle returns true") {
                canHandle(opKey).shouldBeTrue()
            }

            test("from value fails") {
                shouldThrow<IllegalArgumentException> {
                    create(opKey, expected)
                }
            }
            test("from map is successful") {
                create(opKey, mapOf("arg" to expected, "arg2" to expected)).should {
                    it.shouldBeInstanceOf<KeyedOperatorWithMultiMandatoryArg>()
                    it.key shouldBe subKey
                    withEmptyContext {
                        it.arg.invoke() shouldBe expected
                        it.arg2.invoke() shouldBe expected
                    }
                }
            }
            test("build fails") {
                shouldThrow<IllegalArgumentException> {
                    create(opKey)
                }
            }
        }

        context("single optional arg") {
            val opKey = opKey<KeyedOperatorWithSingleOptionalArg>(subKey)

            test("canHandle returns true") {
                canHandle(opKey).shouldBeTrue()
            }

            test("from value is successful") {
                create(opKey, expected).should {
                    it.shouldBeInstanceOf<KeyedOperatorWithSingleOptionalArg>()
                    it.key shouldBe subKey
                    withEmptyContext { it.arg.invoke() shouldBe expected }
                }
            }
            test("from map is successful") {
                create(opKey, mapOf("arg" to expected)).should {
                    it.shouldBeInstanceOf<KeyedOperatorWithSingleOptionalArg>()
                    it.key shouldBe subKey
                    withEmptyContext { it.arg.invoke() shouldBe expected }
                }
            }
            test("build successful") {
                create(opKey).shouldBeInstanceOf<KeyedOperatorWithSingleOptionalArg>()
            }
        }

        context("mutiple optional args") {
            val opKey = opKey<KeyedOperatorWithMultiOptionalArg>(subKey)

            test("canHandle returns true") {
                canHandle(opKey).shouldBeTrue()
            }

            test("from value fails") {
                shouldThrow<IllegalArgumentException> {
                    create(opKey, expected)
                }
            }
            test("from map is successful") {
                create(opKey, mapOf("arg" to expected, "arg2" to expected)).should {
                    it.shouldBeInstanceOf<KeyedOperatorWithMultiOptionalArg>()
                    it.key shouldBe subKey
                    withEmptyContext {
                        it.arg.invoke() shouldBe expected
                        it.arg2.invoke() shouldBe expected
                    }
                }
            }
            test("build successful") {
                create(opKey).shouldBeInstanceOf<KeyedOperatorWithMultiOptionalArg>()
            }
        }
    }


})