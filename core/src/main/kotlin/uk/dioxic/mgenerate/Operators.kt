package uk.dioxic.mgenerate

import org.bson.Document
import java.util.*
import kotlin.random.Random

fun string(
    length: Int,
    characterPool: String = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()[]"
): String {
    val rnd = Random.Default
    val sb = StringBuilder()

    repeat(length) {
        sb.append(characterPool[rnd.nextInt(characterPool.length)])
    }
    return sb.toString()
}

fun number(
    min: Int = 0,
    max: Int = Int.MAX_VALUE
): Int = Random.nextInt(min, max)

fun stringOp(
    length: () -> Int,
    characterPool: () -> String
) = string(length.invoke(), characterPool.invoke())

@uk.dioxic.mgenerate.annotations.Operator
class Text(
    private val length: () -> Int,
    private val characterPool: () -> String = { "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()[]" }
) : () -> String {

    override fun invoke(): String {
        val rnd = Random.Default
        val sb = StringBuilder()
        val pool = characterPool()

        repeat(length()) {
            sb.append(pool[rnd.nextInt(pool.length)])
        }
        return sb.toString()
    }

    override fun toString(): String {
        return "Text(length=${length()}, characterPool=${characterPool()})"
    }

}

//class NumberSequence(
//    start: () -> Int = { 0 },
//    val step: () -> Int = { 1 }
//) : IntOperator {
//
//    private val counter = atomic(start())
//
//    override fun invoke(): Int =
//        counter.addAndGet(step())
//
//}

interface Operator<T> : () -> T {
    override operator fun invoke(): T
}

interface IntOperator : Operator<Int>

interface StringOperator : Operator<String>

class TextBuilder2() {
    private var length: (() -> Int)? = null
    private var characterPool: (() -> String)? = null

    fun length(length: () -> Int) =
        apply { this.length = length }

    fun characterPool(characterPool: () -> String) =
        apply { this.characterPool = characterPool }

    fun document(document: Document) {
        document["length"]?.let {
            length = when (it) {
                is IntOperator -> it
                is Int -> {
                    { it }
                }
                else -> throw IllegalArgumentException("${it::class.java} not valid for length attribute")
            }
        }
        document["characterPool"]?.let {
            characterPool = when (it) {
                is StringOperator -> it
                is String -> {
                    { it }
                }
                else -> throw IllegalArgumentException("${it::class.java} not valid for characterPool attribute")
            }
        }
    }

    fun build(): Text {
        val length = this.length
        val characterPool = this.characterPool

        requireNotNull(length) { "length cannot be null!" }
        return if (characterPool != null) {
            Text(
                length = length,
                characterPool = characterPool
            )
        } else {
            Text(
                length = length
            )
        }
    }
}

//fun <T> Operator<T>.from(document: Document) {
//    val klas = this::class
//    val cons = klas.primaryConstructor
//    val valmap = cons.parameters
//        .associateBy({ it }, { vals[it.name] })
//        .filterNot { it.value == null }
//    return cons.callBy(valmap) as T
//}

fun main() {
    val doc = Document()
    val docMap = HashMap<String, () -> Any>()
//    val stringOperatorClass = Text({ 6 })
//    val stringOperatorClass2 = Text({ 6 }, stringOperatorClass::invoke)
//
//    docMap["k1"] = { stringOp({ 3 }, { "ASDFF" }) }
//    docMap["k2"] = stringOperatorClass::invoke
//
//    docMap.forEach { (k, v) -> println("key: $k, value: ${v.invoke()}") }


//    docMap["length"] = { 6 }
//    docMap["characterPool"] = { "ABC" }
//
//    val text = instance<Text>(docMap)
//
//    println(text)
//    println(text.invoke())
//
//    val text2 = instance<Text> { "sdfsdf" }
//
//    println("Single field : ${instance<Text> { 2 }}")
//    println("Single field : $text2 + result: ${text2()}")


}

data class Customer(
    val id: UUID = UUID.randomUUID(),
    val username: String,
    val auth0Id: String

) {
    class Builder {
        private var id: UUID? = null
        private var username: String? = null
        private var auth0Id: String? = null

        fun id(id: UUID) = apply { this.id = id }
        fun id2(id: UUID) {
            this.id = id
        }

        fun username(username: String) = apply { this.username = username }
        fun auth0Id(auth0Id: String) = apply { this.auth0Id = auth0Id }
        fun build() = Customer(id!!, username!!, auth0Id!!)
    }
}