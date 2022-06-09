package uk.dioxic.mgenerate.operators

import uk.dioxic.mgenerate.exceptions.UnsupportedOperatorAliasException

abstract class AbstractOperatorRegistry(builderMap: Map<String, OperatorBuilder<*>>) : OperatorRegistry {

    private val builderMap: MutableMap<String, OperatorBuilder<*>>

    init {
        this.builderMap = builderMap.toMutableMap()
    }

    override fun getOperatorBuilder(alias: String): OperatorBuilder<*> =
        builderMap[alias] ?: throw UnsupportedOperatorAliasException(alias)

    override fun hasAlias(alias: String) =
        builderMap.containsKey(alias)

    override fun addOperatorBuilder(alias: String, builder: OperatorBuilder<*>) {
        builderMap[alias] = builder
    }

}