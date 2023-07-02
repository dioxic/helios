package uk.dioxic.helios.generate.ejson

import org.bson.json.JsonMode
import org.bson.json.JsonWriterSettings

data class BsonConfiguration internal constructor(
    val encodeDefaults: Boolean = false,
    val ignoreUnknownKeys: Boolean = false,
    val jsonMode: JsonMode = JsonMode.RELAXED,
    val newLineCharacters: String = System.getProperty("line.separator"),
    val allowStructuredMapKeys: Boolean = false,
    val prettyPrint: Boolean = false,
    val explicitNulls: Boolean = true,
    val prettyPrintIndent: String = "  ",
    val coerceInputValues: Boolean = false,
    val useArrayPolymorphism: Boolean = false,
    val classDiscriminator: String = "type",
) {
    fun toJsonWriterSettings(): JsonWriterSettings =
        JsonWriterSettings.builder()
            .indent(prettyPrint)
            .indentCharacters(prettyPrintIndent)
            .outputMode(jsonMode)
            .newLineCharacters(newLineCharacters)
            .build()
}