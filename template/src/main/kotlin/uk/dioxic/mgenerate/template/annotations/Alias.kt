package uk.dioxic.mgenerate.template.annotations

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Alias(
    vararg val aliases: String
)
