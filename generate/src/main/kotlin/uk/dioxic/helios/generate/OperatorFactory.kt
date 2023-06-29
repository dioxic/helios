package uk.dioxic.helios.generate

import org.reflections.Reflections
import uk.dioxic.helios.generate.annotations.Alias
import uk.dioxic.helios.generate.operators.Operator
import uk.dioxic.helios.generate.operators.fakerOperators
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

@Suppress("UNCHECKED_CAST", "MemberVisibilityCanBePrivate")
object OperatorFactory {

    private const val operatorPrefix = "\$"
    private val classMap: MutableMap<String, KClass<out Operator<*>>> = mutableMapOf()
    private val objectMap: MutableMap<String, Operator<*>> = mutableMapOf()

    init {
        // add operator class definitions with reflection
        val reflections = Reflections("uk.dioxic.helios.generate.operators")
        reflections.getSubTypesOf(Operator::class.java)
            .filter { it.isAnnotationPresent(Alias::class.java) }
            .map { it.kotlin as KClass<Operator<*>> }
            .forEach(OperatorFactory::addOperator)

        // add object operators
        fakerOperators.forEach(OperatorFactory::addOperator)
    }

    private fun String.isOperator() =
        startsWith("\$")

    fun addOperator(clazz: KClass<out Operator<*>>) {
        clazz.findAnnotation<Alias>().let { annotation ->
            if (annotation != null && annotation.aliases.isNotEmpty()) {
                annotation.aliases.forEach { alias ->
                    require(!alias.contains('.')) {
                        "Alias cannot contain a '.'"
                    }
                    classMap["$operatorPrefix$alias"] = clazz
                }
            } else {
                classMap["$operatorPrefix${clazz.simpleName}"] = clazz
            }
        }
    }

    fun addOperator(operator: Operator<*>, aliases: List<String>) =
        aliases.forEach { alias ->
            objectMap["$operatorPrefix$alias"] = operator
        }

    fun splitKey(key: String): Pair<String, String> =
        Pair(key.substringBefore('.'), key.substringAfter('.', ""))

    fun canHandle(key: String): Boolean {
        val (rootKey, _) = splitKey(key)
        return rootKey.isOperator() &&
                (classMap.containsKey(rootKey) || objectMap.containsKey(rootKey))
    }

    fun create(key: String, obj: Any): Operator<*> {
        val (rootKey, subKey) = splitKey(key)
        val operatorClass = getOperatorClass(rootKey)

        return when (obj) {
            is Map<*, *> -> OperatorBuilder.fromMap(operatorClass, obj, subKey)
            else -> OperatorBuilder.fromValue(operatorClass, obj, subKey)
        }
    }

    fun create(key: String): Operator<*> {
        val (rootKey, subKey) = splitKey(key)
        return if (subKey.isEmpty()) {
            objectMap[rootKey] ?: OperatorBuilder.build(getOperatorClass(rootKey))
        } else {
            OperatorBuilder.fromMap(clazz = getOperatorClass(rootKey), subKey = subKey)
        }
    }

    private fun getOperatorClass(key: String): KClass<out Operator<*>> =
        classMap[key] ?: error("No operator found for key $key")

}