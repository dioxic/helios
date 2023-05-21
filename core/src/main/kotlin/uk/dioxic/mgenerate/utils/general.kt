package uk.dioxic.mgenerate.utils

import kotlin.random.Random

fun Random.Default.nextElement(list: List<Any>, weights: List<Int>? = null): Any? {
    when {
        list.isEmpty() -> return null
        weights != null -> {
            require(list.size == weights.size) { "length of array and weights must match" }
            var remainingDistance = nextInt(weights.sum())

            for (i in 0..list.size) {
                remainingDistance -= weights[i]
                if (remainingDistance < 0) {
                    return list[i]
                }
            }
            error("couldn't pick a weighted item!")
        }

        else -> return list[nextInt(list.size)]
    }
}