package uk.dioxic.helios.generate

fun interface Operator<T: Any?> {
    context(OperatorContext)
    operator fun invoke(): T
}