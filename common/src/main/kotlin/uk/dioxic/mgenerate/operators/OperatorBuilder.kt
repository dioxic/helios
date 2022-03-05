package uk.dioxic.mgenerate.operators

interface OperatorBuilder<T: () -> Any> {

    fun map(map: Map<String, Any>): OperatorBuilder<T>

    fun singleValue(value: Any): OperatorBuilder<T>

    fun build(): T

    fun hasDefault(): Boolean

    fun canHandleSingleValue(): Boolean

}