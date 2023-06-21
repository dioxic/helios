package uk.dioxic.mgenerate.template.operators

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf

class FakerOperatorTests : FunSpec({

    val operatorMap = fakerOperators.flatMap { (k, v) -> v.map { it to k } }.toMap()

    test("first name") {
        val operator = operatorMap["first"]
        operator.shouldNotBeNull()
        operator().shouldBeInstanceOf<String>()
    }

})