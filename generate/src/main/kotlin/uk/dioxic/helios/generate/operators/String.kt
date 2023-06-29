package uk.dioxic.helios.generate.operators

import uk.dioxic.helios.generate.Operator
import uk.dioxic.helios.generate.OperatorContext
import uk.dioxic.helios.generate.annotations.Alias

@Alias("concat", "join")
class Concat(
    val array: Operator<List<String>> = Operator { emptyList() },
    val separator: Operator<String> = Operator { "" }
) : Operator<String> {
    context (OperatorContext)
    override fun invoke(): String =
        array().joinToString(separator())
}