package uk.dioxic.mgenerate

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