package uk.dioxic.helios.generate

fun interface Wrapped <T: Any?> {

    context(OperatorContext)
    operator fun invoke(): T

}