package uk.dioxic.mgenerate.template.operators

@uk.dioxic.mgenerate.template.annotations.Alias("concat", "join")
class Concat(
    val array: () -> List<String> = { emptyList() },
    val separator: () -> String = { "" }
) : Operator<String> {
    override fun invoke(): String = array().joinToString(separator())
}