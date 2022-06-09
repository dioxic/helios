package uk.dioxic.mgenerate.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class OperatorBuilder(
    vararg val aliases: String
)
