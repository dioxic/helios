package uk.dioxic.helios.generate.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Alias(
    vararg val aliases: String
)
