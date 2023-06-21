package uk.dioxic.mgenerate.template.operators.general

import org.bson.BsonBinary
import org.bson.BsonBinarySubType
import uk.dioxic.mgenerate.template.annotations.Alias
import uk.dioxic.mgenerate.template.operators.Operator
import kotlin.random.Random

@Alias("bin")
class BinaryOperator(
    val size: () -> Int = { 1024 },
    val subtype: () -> BsonBinarySubType = { BsonBinarySubType.BINARY }
) : Operator<BsonBinary> {

    override fun invoke(): BsonBinary =
        BsonBinary(subtype(), Random.nextBytes(size()))
}