package uk.dioxic.mgenerate.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Operator(
    val name: Array<String> = []
)
