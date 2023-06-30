package uk.dioxic.helios.generate.operators

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import uk.dioxic.helios.generate.test.withEmptyContext

class FakerOperatorTests : FunSpec({

    val operatorMap = fakerGenerators.flatMap { (k, v) -> v.map { it to k } }.toMap()

    test("first name") {
        withEmptyContext {
            val operator = operatorMap["first"]
            operator.shouldNotBeNull()
            operator().shouldBeInstanceOf<String>()
        }
    }

})