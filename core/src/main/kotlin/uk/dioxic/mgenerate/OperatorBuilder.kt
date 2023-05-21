package uk.dioxic.mgenerate

import uk.dioxic.mgenerate.operators.Operator
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure

object OperatorBuilder {

    fun <T : Operator<*>> fromMap(clazz: KClass<T>, map: Map<*, *>): T {
        val args = mutableMapOf<KParameter, Any>()
        clazz.primaryConstructor?.valueParameters?.forEach { parameter ->
            if (parameter.type.jvmErasure == Function0::class) {
                val desiredType = parameter.type.arguments.first().type
                map[parameter.name]?.also { value ->
                    args[parameter] = wrap(value, desiredType)
                }
            } else {
                require(parameter.isOptional) {
                    "The internal parameter '${parameter.name}' is not optional"
                }
            }
        }
        return clazz.primaryConstructor!!.callBy(args)
    }

    fun <T : Operator<*>> fromValue(clazz: KClass<T>, value: Any): T {
        val primaryArg = clazz.primaryConstructor?.valueParameters?.first { !it.isOptional }
        require(primaryArg != null) { "primary constructor must have at least one non-optional argument" }
        val desiredType = primaryArg.type.arguments.first().type
        return clazz.primaryConstructor?.callBy(mapOf(primaryArg to wrap(value, desiredType)))!!
    }

    private fun wrap(obj: Any, type: KType?): Function<*> {
        return when (obj) {
            is Function<*> -> obj
            else -> {
                val tObj = type?.let { convert(obj, type) } ?: obj
                { tObj }
            }
        }
    }

    private fun convert(obj: Any, type: KType): Any =
        obj::class.starProjectedType.run {
            when {
                isSubtypeOf(type) -> obj
                isSubtypeOf(Long::class.starProjectedType) && obj is Number -> obj.toLong()
                isSubtypeOf(Int::class.starProjectedType) && obj is Number -> obj.toInt()
                isSubtypeOf(Double::class.starProjectedType) && obj is Number -> obj.toDouble()
                isSubtypeOf(Float::class.starProjectedType) && obj is Number -> obj.toFloat()
                else -> error("Cannot convert ${obj::class} to type of $type")
            }
        }

    fun <T : Operator<*>> build(clazz: KClass<T>): T {
        val mandatoryParameters = clazz.primaryConstructor
            ?.valueParameters
            ?.sumOf { if (it.isOptional) 0 as Int else 1 as Int } ?: 0

        require(mandatoryParameters == 0) { "${clazz.simpleName} has no default configuration" }

        return clazz.createInstance()
    }

}