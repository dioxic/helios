package uk.dioxic.helios.execute

import io.kotest.matchers.shouldBe

infix fun String.shouldBeJson(expected: String?): String {
//    println(this)
    return this.conform() shouldBe expected?.conform()
}

private fun String.conform() = this
    .replace("\n", "")
    .replace(": ", ":")
    .replace(", ", ",")
    .replace("{ ", "{")
    .replace(" }", "}")