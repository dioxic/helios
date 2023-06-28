package uk.dioxic.helios.generate

import uk.dioxic.helios.generate.operators.KeyedOperator
import uk.dioxic.helios.generate.operators.Operator
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure

object OperatorBuilder {

    fun <T : Operator<*>> fromMap(
        clazz: KClass<out T>,
        map: Map<*, *> = emptyMap<Any, Any>(),
        subKey: String? = null
    ): T {
        val args = mutableMapOf<KParameter, Any>()
        clazz.primaryConstructor.let { c ->
            require(c != null) {
                "${clazz.simpleName} must have a primary constructor"
            }
            c.valueParameters.forEach { parameter ->
                if (parameter.name == KeyedOperator.KEY_NAME && subKey != null) {
                    args[parameter] = subKey
                } else {
                    map[parameter.name].also { mapVal ->
                        if (mapVal == null) {
                            require(parameter.isOptional) {
                                "${clazz.simpleName} constructor parameter '${parameter.name}' is not optional"
                            }
                        } else {
                            args[parameter] = convert(parameter, mapVal)
                        }
                    }
                }
            }
        }
        return clazz.primaryConstructor!!.callBy(args.toMutableMap())
    }

    private fun convert(parameter: KParameter, value: Any): Any =
        if (parameter.type.jvmErasure == Function0::class) {
            val desiredType = parameter.type.arguments.first().type
            wrap(value, desiredType)
        } else {
            convert(value, parameter.type)
        }

    private fun KClass<out Operator<*>>.getSingleValueParameter(excludeOpKey: Boolean): KParameter =
        primaryConstructor.let { constructor ->
            require(constructor != null) {
                "$simpleName must have a primary constructor"
            }
            val parameters = constructor.valueParameters.let { p ->
                if (excludeOpKey) {
                    p.filterNot { it.name == KeyedOperator.KEY_NAME }
                } else {
                    p
                }
            }

            if (parameters.size == 1) {
                parameters.first()
            } else {
                require(parameters.count { !it.isOptional } == 1) {
                    "$simpleName constructor must have either a single parameter or only one mandatory parameter"
                }
                parameters.first { !it.isOptional }
            }
        }

    private fun KClass<out Operator<*>>.getKeyParameter(): KParameter =
        primaryConstructor.let { constructor ->
            require(constructor != null) {
                "$simpleName must have a primary constructor"
            }
            constructor.valueParameters.first { it.name == KeyedOperator.KEY_NAME }
        }

    fun <T : Operator<*>> fromValue(clazz: KClass<T>, value: Any, subKey: String = ""): T {
        val primaryParameter = clazz.getSingleValueParameter(subKey.isNotEmpty())
        val primaryValue = convert(primaryParameter, value)

        val args = if (subKey.isNotEmpty()) {
            require(clazz.isSubclassOf(KeyedOperator::class))
            mapOf(
                clazz.getKeyParameter() to subKey,
                primaryParameter to primaryValue
            )
        } else {
            mapOf(primaryParameter to primaryValue)
        }

        return clazz.primaryConstructor?.callBy(args)!!
    }

//    fun <T : Operator<*>> fromValue(clazz: KClass<T>, value: Any): T {
//        val primaryParameter = clazz.getSingleValueParameter(false)
//        val primaryValue = convert(primaryParameter, value)
//
//        return clazz.primaryConstructor?.callBy(mapOf(primaryParameter to primaryValue))!!
//    }

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

    @Suppress("USELESS_CAST")
    fun <T : Operator<*>> build(clazz: KClass<T>): T {
        val mandatoryParameters = clazz.primaryConstructor
            ?.valueParameters
            ?.sumOf { if (it.isOptional) 0 as Int else 1 as Int } ?: 0

        require(mandatoryParameters == 0) { "${clazz.simpleName} has no default configuration" }

        return clazz.createInstance()
    }

}