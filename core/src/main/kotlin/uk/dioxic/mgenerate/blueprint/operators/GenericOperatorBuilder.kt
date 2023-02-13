package uk.dioxic.mgenerate.blueprint.operators

import uk.dioxic.mgenerate.operators.Operator
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.valueParameters

object GenericOperatorBuilder {

    fun somethin(map: Map<*, *>): ChooseOperator {
        val arguments = mutableListOf<Any?>()
        ::ChooseOperator.valueParameters.forEach {
            arguments.add(map[it.name])
        }
        return ChooseOperator::class.primaryConstructor!!.call(*arguments.toTypedArray())
    }

    fun <T : Operator<out Any>> fromMap(clazz: KClass<T>, map: Map<*, *>): T {
        val args = mutableMapOf<KParameter, Any>()
        clazz.primaryConstructor?.valueParameters?.forEach { parameter ->
            map[parameter.name]?.also { value ->
                args[parameter] = wrap(value)
            }
        }
        return clazz.primaryConstructor!!.callBy(args)
    }

    fun <T : Operator<out Any>> fromValue(clazz: KClass<T>, value: Any): T {
        val primaryArg = clazz.primaryConstructor?.valueParameters?.first { !it.isOptional }
        require(primaryArg != null) { "primary constructor must have at least one non-optional argument" }
        return clazz.primaryConstructor?.callBy(mapOf(primaryArg to wrap(value)))!!
    }

    private fun wrap(obj: Any): Function<*> =
        when (obj) {
            is Function<*> -> obj
            else -> {
                { obj }
            }
        }

//    fun toDocument(operator: Operator<Any>): Document {
//        val doc = Document()
//        operator::class.primaryConstructor?.valueParameters?.forEach { parameter ->
//            doc[parameter.name] = operator::class.memberProperties
//                .find { it.name == parameter.name }
//                ?.invoke()
//        }
//        return doc
//    }

    fun <T : Operator<out Any>> build(clazz: KClass<T>): T {
        val mandatoryParameters = clazz.primaryConstructor
            ?.valueParameters
            ?.sumOf { if (it.isOptional) 0 as Int else 1 as Int } ?: 0

        require(mandatoryParameters == 0) { "${clazz.simpleName} has no default configuration" }

        return clazz.createInstance()
    }

}