package uk.dioxic.mgenerate.operator

interface Operator<T> : () -> T {
    override operator fun invoke(): T
}