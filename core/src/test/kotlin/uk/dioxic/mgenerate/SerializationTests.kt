package uk.dioxic.mgenerate

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import uk.dioxic.mgenerate.serializer.*

@ExperimentalSerializationApi
class SerializationTests {

    private val json = Json

    @Test
    fun `rgb serializer`() {
        val colour = RgbColour(23, 34, 54)
        val jsonColour = json.encodeToString(RgbColourSerializer, colour)
        println(jsonColour)
        val actual = json.decodeFromString(RgbColourSerializer, jsonColour)
        println(actual)
        assertThat(actual).isEqualTo(colour)
    }

    @Test
    fun `hex serializer`() {
        val colour = HexColour(toHex(23, 34, 54))
        val jsonColour = json.encodeToString(HexColourSerializer, colour)
        println(jsonColour)
        val actual = json.decodeFromString(HexColourSerializer, """{"g":34,"r":23,"b":54}""")
        println(actual)
        assertThat(actual).isEqualTo(colour)
    }

    @Test
    fun `default serializer`() {
        val colour = RgbColour(23, 34, 54)
        val jsonColour = json.encodeToString(RgbColour.serializer(), colour)
        println(jsonColour)
        assertThat(json.decodeFromString(RgbColour.serializer(), jsonColour)).isEqualTo(colour)
    }
}