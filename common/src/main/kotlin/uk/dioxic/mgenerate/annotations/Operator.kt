package uk.dioxic.mgenerate.annotations

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Operator(
    vararg val aliases: String
)
