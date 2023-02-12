package uk.dioxic.mgenerate.operators

import org.bson.Document
import kotlin.reflect.KClass

interface OperatorBuilder<T : Operator<*>> {

    fun fromDocument(document: Document): T

    fun fromMap(map: Map<*, *>): T

    fun fromValue(value: Any): T

    fun toDocument(operator: T): Document

//    fun singleValue(value: Any): OperatorBuilder<T>

    fun build(): T

    val operatorClass: KClass<T>

//    fun hasDefault(): Boolean

//    fun canHandleSingleValue(): Boolean

}