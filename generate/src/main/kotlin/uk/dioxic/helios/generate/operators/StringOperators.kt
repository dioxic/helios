package uk.dioxic.helios.generate.operators

import uk.dioxic.helios.generate.annotations.Alias

@Alias("concat", "join")
class Concat(
    val array: () -> List<String> = { emptyList() },
    val separator: () -> String = { "" }
) : Operator<String> {
    override fun invoke(): String = array().joinToString(separator())
}