package uk.dioxic.mgenerate.blueprint.operators

import org.bson.Document
import uk.dioxic.mgenerate.annotations.Alias
import uk.dioxic.mgenerate.operators.Operator
import uk.dioxic.mgenerate.operators.OperatorBuilder
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.valueParameters

@Alias("choose")
data class ChooseOperator(
    val from: () -> List<Any>,
    val weights: () -> List<Int>? = { null }
) : Operator<Any> {

    override fun invoke() =
        choose(from(), weights())
}

object ChooseOperatorBuilder : OperatorBuilder<ChooseOperator> {

    fun somethin(map: Map<*, *>): ChooseOperator {
        val arguments = mutableListOf<Any?>()
        ::ChooseOperator.valueParameters.forEach {
            arguments.add(map[it.name])
        }
        return ChooseOperator::class.primaryConstructor!!.call(*arguments.toTypedArray())
    }

    override fun fromDocument(document: Document) = ChooseOperator(
        from = { emptyList() },// document.getList("from", Any::class.java, emptyList()),
        weights = { null } //document.getList("weights", Int::class.java)
    )

    @Suppress("UNCHECKED_CAST")
    override fun fromMap(map: Map<*, *>)= ChooseOperator(
        from = { emptyList() },//map["from"] as? List<Any> ?: emptyList(),
        weights = { null } //map["weights"] as? List<Int>
    )

    override fun fromValue(value: Any): ChooseOperator {
        require(value is List<*>) { "single field must be a list!" }
        return ChooseOperator({listOf(value)}, {null})
    }

    override fun toDocument(operator: ChooseOperator): Document {
        val doc = Document()
        doc["from"] = operator.from
        doc["weights"] = operator.weights
        return doc
    }

    override fun build(): ChooseOperator {
        error("choose operator has no default")
    }

    override val operatorClass: KClass<ChooseOperator>
        get() = ChooseOperator::class
}

fun <T : Any> choose(
    from: List<T>,
    weights: List<Int>? = null
): T {
    require(weights == null || from.size == weights.size) { "length of array and weights must match" }
    if (weights != null) {
        var remainingDistance = Random.nextInt(weights.sum())

        for (i in 0..from.size) {
            remainingDistance -= weights[i]
            if (remainingDistance < 0)
                return from[i]
        }
        error("couldn't pick a weighted item!")
    } else {
        return from[Random.nextInt(from.size)]
    }
}