package uk.dioxic.mgenerate.operators

import uk.dioxic.mgenerate.annotations.Alias
import uk.dioxic.mgenerate.annotations.Operator

@Alias("concat", "join")
class Concat(
    val array: () -> List<String> = { emptyList() },
    val separator: () -> String = { "" }
) : Operator<String> {
    override fun invoke(): String = array().joinToString(separator())
}