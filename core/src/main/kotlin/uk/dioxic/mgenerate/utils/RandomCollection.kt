package uk.dioxic.mgenerate.utils

import java.util.*
import kotlin.random.Random

class RandomCollection<E> (private val random: Random = Random.Default) {
    private val map: NavigableMap<Double, E> = TreeMap()
    private var total = 0.0

    fun add(weight: Double, result: E): RandomCollection<E> {
        if (weight <= 0) return this
        total += weight
        map[total] = result
        return this
    }

    operator fun next(): E {
        val value = random.nextDouble() * total
        return map.higherEntry(value).value
    }
}