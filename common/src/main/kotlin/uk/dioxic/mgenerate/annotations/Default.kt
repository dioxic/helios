package uk.dioxic.mgenerate.annotations

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class Default(
    val value: Int
)
