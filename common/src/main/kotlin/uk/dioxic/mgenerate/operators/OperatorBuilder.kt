package uk.dioxic.mgenerate.operators

import uk.dioxic.mgenerate.TypedFunction

interface OperatorBuilder<T: TypedFunction> {

    fun from(map: Map<String, Any?>): T

//    fun singleValue(value: Any): OperatorBuilder<T>

    fun build(): T

    fun hasDefault(): Boolean

//    fun canHandleSingleValue(): Boolean

}