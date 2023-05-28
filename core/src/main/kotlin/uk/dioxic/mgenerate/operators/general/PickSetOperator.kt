package uk.dioxic.mgenerate.operators.general

import org.apache.logging.log4j.kotlin.logger
import uk.dioxic.mgenerate.annotations.Alias
import uk.dioxic.mgenerate.annotations.Operator
import uk.dioxic.mgenerate.utils.nextElement
import kotlin.math.min
import kotlin.random.Random

@Alias("pickSet")
class PickSetOperator(
    val from: () -> List<Any>,
    val weights: () -> List<Int>? = { null },
    val quantity: () -> Int = { 1 },
    private val slippage: Int = 10
) : Operator<Set<Any>> {

    private val logger = logger()

    override fun invoke(): Set<Any> {
        val from = this.from()
        val quantity = min(this.quantity(), from.size)
        val weights = this.weights()

        val set = mutableSetOf<Any>()
        var iterations = 0
        val maxMisses = quantity * slippage
        // We may generate duplicates, but we don't know if the underlying gen has sufficient cardinality
        // to satisfy our range, so we can try for a while, but must not try forever.
        // The slippage factor controls how many times we will accept a non-unique element before giving up,
        // which is the number of elements in the target set * slippage
        while (iterations < maxMisses && set.size < quantity) {
            val size = set.size
            Random.nextElement(from, weights)?.let {
                set.add(it)
            }
            if (set.size == size) iterations++
        }
        if(set.size != quantity) {
            logger.trace {
                "the target size requirement of $quantity could not be satisfied after $iterations consecutive samples"
            }
        }

        return set
    }

    override fun toString(): String {
        return "PickSetOperator(from=${from.invoke()}, weights=${weights.invoke()}, quantity=${quantity.invoke()})"
    }

}