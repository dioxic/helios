package uk.dioxic.mgenerate.blueprint.operators

import uk.dioxic.mgenerate.annotations.Alias
import uk.dioxic.mgenerate.operators.Operator
import kotlin.random.Random
import kotlin.random.nextInt

@Alias("int")
data class NumberOperator(
    val min: () -> Int = { 0 },
    val max: () -> Int = { Int.MAX_VALUE }
) : Operator<Int> {
    override fun invoke(): Int = Random.nextInt(min()..max())
}

//object NumberOperatorBuilder : OperatorBuilder<NumberOperator> {
//    override fun fromDocument(document: Document) = NumberOperator(
//        min = document.getInteger("min", 0),
//        max = document.getInteger("max", Int.MAX_VALUE)
//    )
//
//    override fun fromMap(map: Map<*, *>) = NumberOperator(
//        min = map["min"] as? Int ?: 0,
//        max = map["max"] as? Int ?: Int.MAX_VALUE
//    )
//
//    override fun fromValue(value: Any): NumberOperator {
//        require(value is Int) { "single field must be an integer!" }
//        return NumberOperator(max = value)
//    }
//
//    override fun toDocument(operator: NumberOperator): Document {
//        val doc = Document()
//        doc["min"] = operator.min
//        doc["max"] = operator.max
//        return doc
//    }
//
//    override fun build(): NumberOperator =
//        NumberOperator()
//
//    override val operatorClass: KClass<NumberOperator>
//        get() = NumberOperator::class
//}

