package uk.dioxic.helios.generate.operators

import uk.dioxic.helios.generate.Operator
import uk.dioxic.helios.generate.OperatorContext
import uk.dioxic.helios.generate.Wrapped
import uk.dioxic.helios.generate.annotations.Alias

@Alias("concat", "join")
class Concat(
    val array: Wrapped<List<String>> = Wrapped { emptyList() },
    val separator: Wrapped<String> = Wrapped { "" }
) : Operator<String> {
    context (OperatorContext)
    override fun invoke(): String =
        array().joinToString(separator())
}