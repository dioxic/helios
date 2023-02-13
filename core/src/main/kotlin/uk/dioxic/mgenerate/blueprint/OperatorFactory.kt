package uk.dioxic.mgenerate.blueprint

import org.reflections.Reflections
import uk.dioxic.mgenerate.annotations.Alias
import uk.dioxic.mgenerate.blueprint.operators.GenericOperatorBuilder
import uk.dioxic.mgenerate.operators.Operator
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

private fun isOperatorKey(key: String) =
    key.startsWith("\$")

private fun getOperatorKey(key: String) =
    key.substring(1).lowercase()

object OperatorFactory {

    private val operatorMap: MutableMap<String, KClass<Operator<*>>> = mutableMapOf()

    init {
        addOperators("uk.dioxic.mgenerate.blueprint")
    }

    @Suppress("UNCHECKED_CAST")
    fun addOperators(packageName: String) {
        val reflections = Reflections(packageName)
        reflections.getSubTypesOf(Operator::class.java)
            .filter { it.isAnnotationPresent(Alias::class.java) }
            .map { it.kotlin as KClass<Operator<*>> }
            .forEach(::addOperator)
    }

    fun addOperator(clazz: KClass<Operator<*>>) {
        clazz.findAnnotation<Alias>()?.aliases?.forEach { alias ->
            operatorMap[alias] = clazz
        }
    }

    fun canHandle(operatorKey: String) =
        isOperatorKey(operatorKey) && operatorMap.containsKey(getOperatorKey(operatorKey))

    fun create(operatorKey: String, obj: Any): Operator<*> {
        val operatorClass = getOperatorClass(operatorKey)

        return when (obj) {
            is Map<*, *> -> GenericOperatorBuilder.fromMap(operatorClass, obj)
            else -> GenericOperatorBuilder.fromValue(operatorClass, obj)
        }
    }

    fun create(operatorKey: String): Operator<*> =
        GenericOperatorBuilder.build(getOperatorClass(operatorKey))

    private fun getOperatorClass(operatorKey: String): KClass<Operator<*>> {
        val clazz = operatorMap[getOperatorKey(operatorKey)]
        requireNotNull(clazz) { "No operator found for key $operatorKey" }
        return clazz
    }

}