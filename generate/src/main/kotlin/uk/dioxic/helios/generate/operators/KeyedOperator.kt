package uk.dioxic.helios.generate.operators

abstract class KeyedOperator<T> : Operator<T> {
    abstract val key: String

    companion object {
        const val KEY_NAME: String = "key"
    }
}
