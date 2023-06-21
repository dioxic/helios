package uk.dioxic.mgenerate.template.operators.general

import uk.dioxic.mgenerate.template.annotations.Alias
import uk.dioxic.mgenerate.template.extensions.nextElement
import uk.dioxic.mgenerate.template.operators.Operator
import kotlin.random.Random

@Alias("choose")
class ChooseOperator(
    val from: () -> List<Any>,
    val weights: () -> List<Int>? = { null }
) : Operator<Any?> {

    override fun invoke(): Any? =
        Random.nextElement(from(), weights())
}

//fun next(list: List<Any>, weights: List<Int>? = null): Any {
//    if (weights != null) {
//        require(list.size == weights.size) { "length of array and weights must match" }
//        var remainingDistance = Random.nextInt(weights.sum())
//
//        for (i in 0..list.size) {
//            remainingDistance -= weights[i]
//            if (remainingDistance < 0)
//                return list[i] as Any
//        }
//        error("couldn't pick a weighted item!")
//    } else {
//        return list[Random.nextInt(list.size)] as Any
//    }
//}