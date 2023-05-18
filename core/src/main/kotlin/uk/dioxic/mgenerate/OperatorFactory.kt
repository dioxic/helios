package uk.dioxic.mgenerate

import org.reflections.Reflections
import uk.dioxic.mgenerate.annotations.Alias
import uk.dioxic.mgenerate.operators.Operator
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation


@Suppress("UNCHECKED_CAST")
object OperatorFactory {

    private val operatorMap: MutableMap<String, KClass<Operator<*>>> = mutableMapOf()

    init {
        val reflections = Reflections("uk.dioxic.mgenerate.operators")
        reflections.getSubTypesOf(Operator::class.java)
            .filter { it.isAnnotationPresent(Alias::class.java) }
            .map { it.kotlin as KClass<Operator<*>> }
            .forEach(OperatorFactory::addOperator)
    }

    private fun isOperatorKey(key: String) =
        key.startsWith("\$")

    private fun getOperatorKey(key: String) =
        key.substring(1).lowercase()

    private fun addOperator(clazz: KClass<Operator<*>>) {
        clazz.findAnnotation<Alias>()?.aliases?.forEach { alias ->
            operatorMap[alias] = clazz
        }
    }

    fun canHandle(operatorKey: String) =
        isOperatorKey(operatorKey) && operatorMap.containsKey(getOperatorKey(operatorKey))

    fun create(operatorKey: String, obj: Any): Operator<*> {
        val operatorClass = getOperatorClass(operatorKey)

        return when (obj) {
            is Map<*, *> -> OperatorBuilder.fromMap(operatorClass, obj)
            else -> OperatorBuilder.fromValue(operatorClass, obj)
        }
    }

    fun create(operatorKey: String): Operator<*> =
        OperatorBuilder.build(getOperatorClass(operatorKey))

    private fun getOperatorClass(operatorKey: String): KClass<Operator<*>> =
        operatorMap[getOperatorKey(operatorKey)] ?: error("No operator found for key $operatorKey")

}