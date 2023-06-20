package uk.dioxic.mgenerate.template.operators.general

import uk.dioxic.mgenerate.template.operators.Operator

@uk.dioxic.mgenerate.template.annotations.Alias("pick")
class PickOperator(
    val array: () -> List<Any>,
    val element: () -> Int = { 0 },
) : Operator<Any?> {
    override fun invoke(): Any? {
        val a = array()
        val i = element()

        require(i >= 0) {
            "element must be positive"
        }

        return when {
            a.isEmpty() -> null
            i < a.size -> a[i]
            else -> a[0]
        }
    }
}