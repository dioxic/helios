package uk.dioxic.helios.generate.operators

import org.apache.logging.log4j.kotlin.logger
import org.bson.BsonBinary
import org.bson.BsonBinarySubType
import org.bson.types.ObjectId
import uk.dioxic.helios.generate.Operator
import uk.dioxic.helios.generate.OperatorContext
import uk.dioxic.helios.generate.annotations.Alias
import uk.dioxic.helios.generate.extensions.nextElement
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

@Alias("array")
class ArrayOperator(
    val of: Operator<Any>,
    val number: Operator<Int> = Operator { 5 }
) : Operator<List<*>> {

    context (OperatorContext)
    override fun invoke(): List<*> =
        List(max(number(), 0)) {
            of()
        }
}

@Alias("choose")
class ChooseOperator(
    val from: Operator<List<Any>>,
    val weights: Operator<List<Int>?> = Operator { null }
) : Operator<Any?> {

    context (OperatorContext)
    override fun invoke(): Any? =
        Random.nextElement(from(), weights())
}

@Alias("pick")
class PickOperator(
    val array: Operator<List<Any>>,
    val element: Operator<Int> = Operator { 0 },
) : Operator<Any?> {

    context(OperatorContext)
    override fun invoke(): Any? {
        val a = array()
        val i = element()

        require(i >= 0) {
            "element must be positive"
        }

        return when {
            a.isEmpty() -> null
            i < a.size -> a[i]
            else -> a[0]
        }
    }
}

@Alias("bool")
class BooleanOperator(
    val probability: Operator<Double> = Operator { 0.5 }
) : Operator<Boolean> {

    context (OperatorContext)
    override fun invoke(): Boolean =
        Random.nextDouble(0.0, probability()) > 0.5
}

@Alias("bin")
class BinaryOperator(
    val size: Operator<Int> = Operator { 1024 },
    val subtype: Operator<BsonBinarySubType> = Operator { BsonBinarySubType.BINARY }
) : Operator<BsonBinary> {

    context (OperatorContext)
    override fun invoke(): BsonBinary =
        BsonBinary(subtype(), Random.nextBytes(size()))
}

@Alias("pickSet")
class PickSetOperator(
    val from: Operator<List<Any>>,
    val weights: Operator<List<Int>?> = Operator { null },
    val quantity: Operator<Int> = Operator { 1 },
    private val slippage: Int = 10
) : Operator<Set<Any>> {

    private val logger = logger()

    context(OperatorContext)
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
        if (set.size != quantity) {
            logger.trace {
                "the target size requirement of $quantity could not be satisfied after $iterations consecutive samples"
            }
        }

        return set
    }

}

class UuidOperator(
    val type: Operator<String> = Operator { "BINARY" }
) : Operator<Any> {
    context(OperatorContext)
    override fun invoke(): Any =
        UUID.randomUUID().let { uuid ->
            val type = type()
            when (type().uppercase()) {
                "BINARY" -> uuid
                "STRING" -> uuid.toString()
                else -> error("uuid type [$type] not recognised")
            }
        }
}

@Alias("objectId")
class ObjectIdOperator : Operator<ObjectId> {

    context (OperatorContext)
    override fun invoke(): ObjectId =
        ObjectId.get()
}

class OptionalOperator(val value: Operator<Any>) : Operator<Any> {

    context(OperatorContext)
    override fun invoke(): Any = value()
}