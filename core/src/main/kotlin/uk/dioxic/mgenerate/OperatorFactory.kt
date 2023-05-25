package uk.dioxic.mgenerate

import org.reflections.Reflections
import uk.dioxic.mgenerate.annotations.Alias
import uk.dioxic.mgenerate.operators.Operator
import uk.dioxic.mgenerate.operators.noInputOperators
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

@Suppress("UNCHECKED_CAST")
object OperatorFactory {

    private const val operatorPrefix = "\$"
    private val classMap: MutableMap<String, KClass<Operator<*>>> = mutableMapOf()
    private val objectMap: MutableMap<String, Operator<*>> = mutableMapOf()

    init {
        // add operator class definitions with reflection
        val reflections = Reflections("uk.dioxic.mgenerate.operators")
        reflections.getSubTypesOf(Operator::class.java)
            .filter { it.isAnnotationPresent(Alias::class.java) }
            .map { it.kotlin as KClass<Operator<*>> }
            .forEach(OperatorFactory::addOperator)

        // add object operators
        objectMap.putAll(noInputOperators)
    }

    private fun String.isOperator() =
        startsWith("\$")

    private fun addOperator(clazz: KClass<Operator<*>>) {
        clazz.findAnnotation<Alias>()?.aliases?.forEach { alias ->
            classMap["$operatorPrefix$alias"] = clazz
        }
    }

    fun canHandle(key: String) = key.isOperator() &&
            (classMap.containsKey(key) || objectMap.containsKey(key))

    fun create(key: String, obj: Any): Operator<*> {
        val operatorClass = getOperatorClass(key)

        return when (obj) {
            is Map<*, *> -> OperatorBuilder.fromMap(operatorClass, obj)
            else -> OperatorBuilder.fromValue(operatorClass, obj)
        }
    }

    fun create(key: String): Operator<*> =
        objectMap[key] ?: OperatorBuilder.build(getOperatorClass(key))

    private fun getOperatorClass(key: String): KClass<Operator<*>> =
        classMap[key] ?: error("No operator found for key $key")

}