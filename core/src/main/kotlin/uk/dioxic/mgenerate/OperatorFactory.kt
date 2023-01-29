package uk.dioxic.mgenerate

import com.squareup.kotlinpoet.asTypeName
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.ExperimentalReflectionOnLambdas
import kotlin.reflect.jvm.reflect

inline fun <reified T : () -> Any> instance(vals: Map<String, () -> Any>): T {

//    val klas: KClass<Operator<Any>> = Operator::class as KClass<Operator>
//    val klas:KClass<Operator<*>> = T::class as KClass<Operator<*>>
//    val klas = T::class
//    val klass = T::class
//    klass.primaryConstructor
    val cons = T::class.primaryConstructor!!
    val valmap = cons.parameters
        .associateBy({ it }, { vals[it.name] })
        .filterNot { it.value == null }
    return cons.callBy(valmap)
}

@OptIn(ExperimentalReflectionOnLambdas::class)
inline fun <reified T : () -> Any> instance(noinline value: () -> Any): T {
    val cons = T::class.primaryConstructor!!

    val mandatoryParameters = cons.parameters
        .filterNot { it.isOptional }

    val singleParameter = mandatoryParameters[0]
    val expectedType = singleParameter.type
    val valueReturnType = value.reflect()?.returnType

    require(expectedType == valueReturnType) {
        "${T::class.simpleName}.${singleParameter.name} expecting [$expectedType] - cannot accept type [$valueReturnType]"
    }

    require(mandatoryParameters.count() <= 1) { "${T::class.simpleName} requires parameters $mandatoryParameters" }

    return cons.callBy(mapOf(singleParameter to value))
}