package uk.dioxic.mgenerate.blueprint

import org.bson.Document
import org.reflections.Reflections
import uk.dioxic.mgenerate.annotations.Alias
import uk.dioxic.mgenerate.operators.Operator
import uk.dioxic.mgenerate.operators.OperatorBuilder
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSuperclassOf

private typealias OperatorBuilderAnnotation = uk.dioxic.mgenerate.annotations.OperatorBuilder

private fun isOperatorKey(key: String) =
    key.startsWith("\$")

private fun getOperatorKey(key: String) =
    key.substring(1).lowercase()

object OperatorFactory {

    private val builderMap: MutableMap<String, OperatorBuilder<*>> = mutableMapOf()

    init {
        addBuilders("uk.dioxic.mgenerate.blueprint")
    }

    @Suppress("UNCHECKED_CAST")
    fun addBuilders(packageName: String) {
        val reflections = Reflections(packageName)
//        reflections.getTypesAnnotatedWith(OperatorBuilderAnnotation::class.java)
        reflections.getSubTypesOf(OperatorBuilder::class.java)
            .map { it.kotlin }
            .filter { OperatorBuilder::class.isSuperclassOf(it) }
            .mapNotNull { it.objectInstance }
            .forEach(::addBuilder)
    }

    fun addBuilder(builder: OperatorBuilder<*>) {
        builder.operatorClass.findAnnotation<Alias>()?.aliases?.forEach { alias ->
            builderMap[alias.lowercase()] = builder
        }
    }

    fun canHandle(operatorKey: String) =
        isOperatorKey(operatorKey) && builderMap.containsKey(getOperatorKey(operatorKey))

    private fun getBuilder(operatorKey: String): OperatorBuilder<*> =
        builderMap[getOperatorKey(operatorKey)] ?: error("no builder found for $operatorKey")

    fun create(operatorKey: String, obj: Any): Operator<*> =
        getBuilder(operatorKey).let { builder ->
            when (obj) {
                is Map<*, *> -> builder.fromMap(obj)
                else -> builder.fromValue(obj)
            }
        }

    fun create(operatorKey: String): Operator<*> =
        getBuilder(operatorKey).build()

}