package uk.dioxic.mgenerate.operator

//fun string(
//    length: Int,
//    characterPool: String = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()[]"
//): String {
//    val rnd = Random.Default
//    val sb = StringBuilder()
//
//    repeat(length) {
//        sb.append(characterPool[rnd.nextInt(characterPool.length)])
//    }
//    return sb.toString()
//}
//
//fun number(
//    min: Int = 0,
//    max: Int = Int.MAX_VALUE
//): Int = Random.nextInt(min, max)
//
//abstract class OperatorConfig(val onceOnly: Boolean)
//
//class NumberOperatorConfig(val min: Int, val max: Int, onceOnly: Boolean = false) : OperatorConfig(onceOnly)
//
//fun numberCfg(noc: NumberDto) = Random.nextInt(noc.min, noc.max)
//
//sealed class OperatorDto
//data class NumberDto(val min: Int, val max: Int): OperatorDto()
//
//class DtoOperator<R>(val func: (OperatorDto) -> R) {
//    val dto = NumberDto(1,5)
//    fun execute() = func.invoke(dto)
//}
//
//
//interface Operator<R> {
//    fun execute(): R
//}
//
//class NumberOperator(
//    private val min: Operator<Int>,
//    private val max: Operator<Int>,
//    onceOnly: Boolean = false,
//) : AbstractOperator<Int>(onceOnly) {
//
//    override fun executeInternal(): Int =
//        number(min.execute(), max.execute())
//}
//
//class NumberOperatorBuilder {
//    private var min: Operator<Int>? = null
//    private var max: Operator<Int>? = null
//
//    fun min(min: Operator<Int>) = apply { this.min = min }
//
//    fun max(max: Operator<Int>) = apply { this.max = max }
//
//    fun document(document: Document) {
//        document["min"]?.let {
//            min(convertAndWrap(it))
//        }
//
//        document["max"]?.let {
//            max(convertAndWrap(it))
//        }
//    }
//
//    @Suppress("UNCHECKED_CAST")
//    fun convertAndWrap(value: Any): Operator<Int> =
//        when (value) {
//            is Number -> ConstantOperator(value.toInt())
//            is Operator<*> -> value as Operator<Int>
//            else -> throw IllegalArgumentException("cannot convert $value to an Int");
//        }
//
//    fun build(): NumberOperator {
//        return NumberOperator(
//            min = min!!,
//            max = max!!
//        )
//    }
//}
//
//class StringOperator(
//    private val length: Operator<Int>,
//    private val characterPool: Operator<String>,
//    onceOnly: Boolean = false,
//) : AbstractOperator<String>(onceOnly) {
//
//    override fun executeInternal(): String =
//        string(length.execute(), characterPool.execute())
//}
//
//class ConstantOperator<R>(private val value: R) : Operator<R> {
//    override fun execute(): R = value
//}
//
//class GenericOperator<R>(
//    private val function: KFunction<R>,
//    config: Map<String, Operator<Any>>,
//    onceOnly: Boolean = false,
//) : AbstractOperator<R>(onceOnly) {
//    private val args: Map<KParameter, Operator<Any>> = function.valueParameters
//        .filter { config.containsKey(it.name) }
//        .map {
//            it to config[it.name]!!
//        }
//        .toMap()
//
//    override fun executeInternal(): R {
//        val hydratedArgs =
//            args.mapValues { (_, v) -> v.execute() }
//
//        return function.callBy(hydratedArgs)
//    }
//}
//
//abstract class AbstractOperator<R>(
//    private val onceOnly: Boolean = false
//) : Operator<R> {
//
//    private val value: R by lazy {
//        execute()
//    }
//
//    abstract fun executeInternal(): R
//
//    override fun execute(): R =
//        if (onceOnly) {
//            value
//        } else {
//            executeInternal()
//        }
//
//}
//
//fun main() {
//    val numConfig = Document().of(
//        "min" to 5,
//        "max" to 20
//    )
//    val numOperator = GenericOperator(::number, numConfig, false)
//
//    val strConfig = Document().of(
//        "length" to numOperator,
//        "characterPool" to "MONGODB123"
//    )
//
//    val strOperator = GenericOperator(::string, strConfig)
//
//    repeat(5) {
//        println(strOperator.execute())
//    }
//}