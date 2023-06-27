package uk.dioxic.helios.generate.operators.general

import uk.dioxic.helios.generate.operators.Operator
import java.util.*

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