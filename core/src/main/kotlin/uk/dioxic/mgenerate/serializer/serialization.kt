package uk.dioxic.mgenerate.serializer

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class RgbColour(val r: Int, val g: Int, val b: Int)

@Serializable
data class HexColour(val hex: String)

fun Int.toHexCode(): String {
    return Integer.toHexString(this)
}

fun String.red(): Int {
    return Integer.valueOf(this.substring(1, 3), 16)
}

fun String.green(): Int {
    return Integer.valueOf(this.substring(3, 5), 16)
}

fun String.blue(): Int {
    return Integer.valueOf(this.substring(5, 7), 16)
}

fun toHex(r: Int, g: Int, b: Int): String {
    return "#" +
            r.toHexCode() +
            g.toHexCode() +
            b.toHexCode()
}

//class Destructured(private val hexColour: String) {
//    operator fun component1(): Int = Integer.valueOf(hexColour.substring(1,3), 16)
//    operator fun component2(): Int = Integer.valueOf(hexColour.substring(3,5), 16)
//    operator fun component3(): Int = Integer.valueOf(hexColour.substring(5,7), 16)
//}
//
//fun String.toRgb(): Destructured = Destructured(this)

@ExperimentalSerializationApi
@Serializer(forClass = RgbColour::class)
object RgbColourSerializer : KSerializer<RgbColour> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("rgb colour", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: RgbColour) {
        encoder.encodeString(toHex(value.r, value.g, value.b))
    }

    override fun deserialize(decoder: Decoder): RgbColour {
        val code = decoder.decodeString()

        return RgbColour(code.red(), code.green(), code.blue())
    }
}

@ExperimentalSerializationApi
@Serializer(forClass = HexColour::class)
object HexColourSerializer : KSerializer<HexColour> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("hex colour") {
        element("r", PrimitiveSerialDescriptor("red", PrimitiveKind.STRING))
        element("g", PrimitiveSerialDescriptor("green", PrimitiveKind.STRING))
        element("b", PrimitiveSerialDescriptor("blue", PrimitiveKind.STRING))
    }

    override fun deserialize(decoder: Decoder): HexColour {
        var r = 0
        var g = 0
        var b = 0

        val compositeDecoder = decoder.beginStructure(descriptor)
        loop@ while (true) {
            when (val i = compositeDecoder.decodeElementIndex(descriptor)) {
                0 -> r = compositeDecoder.decodeIntElement(descriptor, i)
                1 -> g = compositeDecoder.decodeIntElement(descriptor, i)
                2 -> b = compositeDecoder.decodeIntElement(descriptor, i)
                CompositeDecoder.DECODE_DONE -> break@loop
            }
        }
        compositeDecoder.endStructure(descriptor)
        return HexColour(toHex(r, g, b))
    }

    override fun serialize(encoder: Encoder, value: HexColour) {
        val compositeDecoder = encoder.beginStructure(descriptor)
        compositeDecoder.encodeIntElement(descriptor, 0, value.hex.red())
        compositeDecoder.encodeIntElement(descriptor, 1, value.hex.green())
        compositeDecoder.encodeIntElement(descriptor, 2, value.hex.blue())
        compositeDecoder.endStructure(descriptor)
    }
}

@ExperimentalSerializationApi
@Serializer(forClass = LocalDateTime::class)
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("date ") {
        element("\$date", PrimitiveSerialDescriptor("date", PrimitiveKind.STRING))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        TODO("Not yet implemented")
    }

    //    override fun load(input: KInput): LocalDateTime {
//        return LocalDateTime.parse(input.readStringValue(), DateTimeFormatter.ISO_DATE_TIME)
//    }
//
//    override fun save(output: KOutput, obj: LocalDateTime) {
//        output.writeStringValue(obj.format(DateTimeFormatter.ISO_DATE_TIME))
//    }

}
