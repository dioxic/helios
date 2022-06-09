package uk.dioxic.mgenerate.operators

interface OperatorRegistry {

    fun hasAlias(alias: String): Boolean

    fun getOperatorBuilder(alias: String): OperatorBuilder<*>

    fun addOperatorBuilder(alias: String, builder: OperatorBuilder<*>)

}