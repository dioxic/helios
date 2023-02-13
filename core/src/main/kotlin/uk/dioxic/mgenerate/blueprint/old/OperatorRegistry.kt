package uk.dioxic.mgenerate.blueprint.old

//data class OperatorAlias(
//    val clazz: Class<out TypedFunction>,
//    val aliases: List<String>
//)

//object OperatorRegistry: Transformer {

//    private val aliases = listOf(
//        OperatorAlias(ChooseOperator::class.java, listOf("\$choose"))
//    )
//
//    private val classByAlias = aliases.fold(mutableMapOf<String, Class<out TypedFunction>>()) { map, alias ->
//        alias.aliases.forEach {
//            map[it] = alias.clazz
//        }
//        map
//    }
//
//    private val aliasesByClass = aliases.fold(mutableMapOf<Class<out TypedFunction>, List<String>>()) { map, alias ->
//        map[alias.clazz] = alias.aliases
//        map
//    }
//
//    // will handle the input signature as an operator
//    fun hasAlias(key: String) =
//        classByAlias.containsKey(key)
//
//    fun hasClass(clazz: Class<out TypedFunction>) =
//        aliasesByClass.containsKey(clazz)
//
//    fun getClass(key: String) =
//        classByAlias[key]
//
//    fun getPrimaryAlias(operatorClass: Class<out TypedFunction>) =
//        aliasesByClass[operatorClass]?.firstOrNull()
//
//    override fun transform(objectToTransform: Any?): Any {
//        when(objectToTransform){
//            is Document -> objectToTransform.filterKeys { classByAlias.containsKey(it) }
//                .firstNotNullOfOrNull {  }
//        }
//    }

//}