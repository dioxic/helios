package uk.dioxic.mgenerate.blueprint

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isSuccess
import org.bson.Document
import org.bson.UuidRepresentation
import org.bson.codecs.*
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.bson.codecs.configuration.CodecRegistries.withUuidRepresentation
import org.bson.json.JsonReader
import org.junit.jupiter.api.Test

private val DEFAULT_REGISTRY = fromProviders(
    listOf(
        ValueCodecProvider(),
        CollectionCodecProvider(), IterableCodecProvider(),
        BsonValueCodecProvider(), DocumentCodecProvider(OperatorTransformer()), MapCodecProvider()
    )
)

private val DEFAULT_CODEC = withUuidRepresentation(DEFAULT_REGISTRY, UuidRepresentation.STANDARD)[Document::class.java]


val json = """
    {
        "color": {
            "${'$'}choose": {
                "from": ["blue", "red", "green"]
            }
        },
        "height": 175
    }
""".trimIndent()

class OperatorJsonDecode {

    @Test
    fun decode() {

        val bsonReader = JsonReader(json)

        assertThat { DEFAULT_CODEC.decode(bsonReader, DecoderContext.builder().build()) }
            .isSuccess()
            .isInstanceOf(Document::class)
            .given { actual ->
                assert(actual["color"] is ChooseOperator)
                assert(actual["height"] == 175)
            }

    }

}