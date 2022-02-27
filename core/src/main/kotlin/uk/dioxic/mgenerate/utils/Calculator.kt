package uk.dioxic.mgenerate.utils

class Calculator {

    fun add(a: Int, b: Int): Int {
        return a + b
    }

    fun div(a: Int, b: Int): Double {
        assert(b != 0) { "Division by Zero" }
        return a / b * 1.0
    }

}