package uk.dioxic.mgenerate.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Builder(
    vararg val aliases: String
)
