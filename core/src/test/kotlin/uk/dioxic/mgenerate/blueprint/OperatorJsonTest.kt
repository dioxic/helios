package uk.dioxic.mgenerate.blueprint

import assertk.all
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.key
import org.bson.Document
import org.bson.UuidRepresentation
import org.bson.codecs.*
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.bson.codecs.configuration.CodecRegistries.withUuidRepresentation
import org.bson.json.JsonReader
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import uk.dioxic.mgenerate.blueprint.operators.ChooseOperator
import uk.dioxic.mgenerate.blueprint.operators.NumberOperator

private val DEFAULT_REGISTRY = fromProviders(
    listOf(
        ValueCodecProvider(),
        CollectionCodecProvider(), IterableCodecProvider(),
        BsonValueCodecProvider(), DocumentCodecProvider(OperatorTransformer()), MapCodecProvider()
    )
)

private val DEFAULT_CODEC = withUuidRepresentation(DEFAULT_REGISTRY, UuidRepresentation.STANDARD)[Document::class.java]

private val json = """
    {
        "color": {
            "${'$'}choose": {
                "from": ["blue", "red", "green"]
            }
        },
        "height": "${'$'}int",
        "length": {
            "${'$'}int": {
                "max": 10
            }
        },
        "address": {
            "city" : {
                "${'$'}choose": ["london", "new york", "dulwich"]
            }
        }
    }
""".trimIndent()

class OperatorJsonDecode {

    @Test
    fun decode() {
        val bsonReader = JsonReader(json)

        val actual = assertDoesNotThrow { DEFAULT_CODEC.decode(bsonReader, DecoderContext.builder().build()) }

        println("json: $json")
        println(actual)

        assertThat(actual, "document")
            .isInstanceOf(Document::class)
            .all {
                key("color").isInstanceOf(ChooseOperator::class)
                key("height").isInstanceOf(NumberOperator::class)
                key("address")
                    .isInstanceOf(Document::class)
                    .key("city").isInstanceOf(ChooseOperator::class)
            }
    }

}