package uk.dioxic.helios.generate

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.right
import org.reflections.Reflections
import uk.dioxic.helios.generate.annotations.Alias
import uk.dioxic.helios.generate.exceptions.NoOperatorFound
import uk.dioxic.helios.generate.exceptions.OperatorError
import uk.dioxic.helios.generate.operators.fakerGenerators
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

@Suppress("UNCHECKED_CAST", "MemberVisibilityCanBePrivate")
object OperatorFactory {

    const val operatorPrefix = "\$"
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
        fakerGenerators.forEach(OperatorFactory::addOperator)
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

    fun create(key: String, obj: Any): Either<OperatorError, Operator<*>> = either {
        val (rootKey, subKey) = splitKey(key)
        val operatorClass = ensureNotNull(classMap[rootKey]) {
            raise(NoOperatorFound(rootKey))
        }

        return when (obj) {
            is Map<*, *> -> OperatorBuilder.fromMap(operatorClass, obj, subKey)
            else -> OperatorBuilder.fromValue(operatorClass, obj, subKey)
        }
    }

    fun create(key: String): Either<OperatorError, Operator<*>> = either {
        val (rootKey, subKey) = splitKey(key)
        val operatorClass = classMap[rootKey]

        ensure(operatorClass != null || subKey.isEmpty()) {
            raise(NoOperatorFound(rootKey))
        }

        if (operatorClass == null) {
            return objectMap[rootKey]?.right() ?: NoOperatorFound(rootKey).left()
        }

        return if (subKey.isEmpty()) {
            OperatorBuilder.build(operatorClass)
        } else {
            OperatorBuilder.fromMap(clazz = operatorClass, subKey = subKey)
        }
    }

}