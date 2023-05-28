package uk.dioxic.mgenerate.operators.general

import org.bson.BsonBinary
import org.bson.BsonBinarySubType
import uk.dioxic.mgenerate.annotations.Alias
import uk.dioxic.mgenerate.annotations.Operator
import kotlin.random.Random

@Alias("bin")
class BinaryOperator(
    val size: () -> Int = { 1024 },
    val subtype: () -> BsonBinarySubType = { BsonBinarySubType.BINARY }
) : Operator<BsonBinary> {

    override fun invoke(): BsonBinary =
        BsonBinary(subtype(), Random.nextBytes(size()))
}