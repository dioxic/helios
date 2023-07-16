package uk.dioxic.helios.execute.model

import org.bson.Document
import org.bson.conversions.Bson
import uk.dioxic.helios.generate.OperatorContext

/**
 * A way to get the context and the document to the encoder together.
 */
data class EncodeContext(
    val document: Document,
    val operatorContext: OperatorContext
): Bson by document