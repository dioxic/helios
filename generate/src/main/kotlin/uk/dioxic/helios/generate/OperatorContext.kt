package uk.dioxic.helios.generate

interface OperatorContext {

    operator fun get(key: String): Any?

    companion object {
        val EMPTY = object : OperatorContext {
            override fun get(key: String) = null
        }
    }
}