package uk.dioxic.mgenerate.operators.general

import uk.dioxic.mgenerate.utils.nextElement
import uk.dioxic.mgenerate.operators.Operator
import kotlin.random.Random

class PickSetOperator(
    val from: () -> List<Any>,
    val weights: () -> List<Int>? = { null },
    val quantity: () -> Int = { 1 },
) : Operator<Set<Any>> {

    override fun invoke(): Set<Any> {
        val from = this.from()
        val quantity = this.quantity()
        val weights = this.weights()

        require(quantity <= from.size) { "quantity must be less than or equal to the input list" }

        return mutableSetOf<Any>().apply {
            repeat(from.size) {
                add(Random.nextElement(from, weights))
            }
        }
    }
}