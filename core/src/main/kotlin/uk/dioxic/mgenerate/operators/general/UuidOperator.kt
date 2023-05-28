package uk.dioxic.mgenerate.operators.general

import uk.dioxic.mgenerate.annotations.Operator
import java.util.UUID

class UuidOperator(
    val type: () -> String = { "BINARY" }
) : Operator<Any> {
    override fun invoke(): Any =
        UUID.randomUUID().let { uuid ->
            val type = type()
            when (type().uppercase()) {
                "BINARY" -> uuid
                "STRING" -> uuid.toString()
                else -> error("uuid type [$type] not recognised")
            }
        }
}