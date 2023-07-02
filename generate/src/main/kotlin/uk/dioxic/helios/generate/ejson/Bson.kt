@file:OptIn(ExperimentalSerializationApi::class)

package uk.dioxic.helios.generate.ejson

import kotlinx.serialization.*
import kotlinx.serialization.modules.SerializersModule
import org.bson.AbstractBsonReader
import org.bson.BsonDocument
import org.bson.BsonDocumentWriter
import org.bson.BsonValue
import org.bson.json.JsonMode

/**
 * The main entry point to work with JSON serialization.
 * It is typically used by constructing an application-specific instance, with configured JSON-specific behaviour
 * and, if necessary, registered in [SerializersModule] custom serializers.
 * `Bson` instance can be configured in its `Bson {}` factory function using [BsonBuilder].
 * For demonstration purposes or trivial usages, Bson [companion][Bson.Default] can be used instead.
 *
 * Then constructed instance can be used either as regular [SerialFormat] or [StringFormat]
 * or for converting objects to [BsonValue] back and forth.
 *
 * This is the only serial format which has the first-class [BsonValue] support.
 * Any serializable class can be serialized to or from [BsonValue] with [Bson.decodeFromBsonDocument] and [Bson.encodeToBsonDocument] respectively or
 * serialize properties of [BsonValue] type.
 *
 * Example of usage:
 * ```
 * @Serializable
 * class DataHolder(val id: Int, val data: String, val extensions: BsonValue)
 *
 * val json = Bson
 * val instance = DataHolder(42, "some data", buildBsonObject { put("additional key", "value") }
 *
 * // Plain StringFormat usage
 * val stringOutput: String = json.encodeToString(instance)
 *
 * // BsonValue serialization specific for JSON only
 * val jsonTree: BsonValue = json.encodeToBsonValue(instance)
 *
 * // Deserialize from string
 * val deserialized: DataHolder = json.decodeFromString<DataHolder>(stringOutput)
 *
 * // Deserialize from json tree, JSON-specific
 * val deserializedFromTree: DataHolder = json.decodeFromBsonValue<DataHolder>(jsonTree)
 *
 *  // Deserialize from string to JSON tree, JSON-specific
 *  val deserializedToTree: BsonValue = json.parseToBsonValue(stringOutput)
 * ```
 *
 * Bson instance also exposes its [configuration] that can be used in custom serializers
 * that rely on [BsonDecoder] and [BsonEncoder] for customizable behaviour.
 */
sealed class Bson(
    val configuration: BsonConfiguration,
    override val serializersModule: SerializersModule
) : StringFormat {

    /**
     * The default instance of [Bson] with default configuration.
     */
    companion object Default : Bson(BsonConfiguration(), defaultSerializersModule)

    /**
     * Serializes the [value] into an equivalent JSON using the given [serializer].
     *
     * @throws [SerializationException] if the given value cannot be serialized to JSON.
     */
    final override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String =
        encodeToBsonDocument(serializer, value).toJson(configuration.toJsonWriterSettings())

    /**
     * Decodes and deserializes the given JSON [string] to the value of type [T] using deserializer
     * retrieved from the reified type parameter.
     *
     * @throws SerializationException in case of any decoding-specific error
     * @throws IllegalArgumentException if the decoded input is not a valid instance of [T]
     */
    inline fun <reified T> decodeFromString(string: String): T =
        decodeFromString(serializersModule.serializer(), string)

    /**
     * Deserializes the given JSON [string] into a value of type [T] using the given [deserializer].
     *
     * @throws [SerializationException] if the given JSON string is not a valid JSON input for the type [T]
     * @throws [IllegalArgumentException] if the decoded input cannot be represented as a valid instance of type [T]
     */
    final override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
        return decodeFromBsonDocument(deserializer, BsonDocument.parse(string))
    }
    /**
     * Serializes the given [value] into an equivalent [BsonValue] using the given [serializer]
     *
     * @throws [SerializationException] if the given value cannot be serialized to JSON
     */
    fun <T> encodeToBsonDocument(serializer: SerializationStrategy<T>, value: T): BsonDocument {
        val document = BsonDocument()
        val writer = BsonDocumentWriter(document)
        serializer.serialize(DefaultBsonEncoder(writer, serializersModule, configuration), value)
        writer.flush()
        return document
    }

    /**
     * Deserializes the given [bson] into a value of type [T] using the given [deserializer].
     *
     * @throws [SerializationException] if the given JSON element is not a valid JSON input for the type [T]
     * @throws [IllegalArgumentException] if the decoded input cannot be represented as a valid instance of type [T]
     */
    fun <T> decodeFromBsonDocument(deserializer: DeserializationStrategy<T>, bson: BsonDocument): T {
        val reader = bson.asBsonReader()
        require(reader is AbstractBsonReader)
        return deserializer.deserialize(DefaultBsonDecoder(reader, serializersModule, configuration, this))
    }

    /**
     * Deserializes the given JSON [string] into a corresponding [BsonValue] representation.
     *
     * @throws [SerializationException] if the given string is not a valid JSON
     */
    fun parseToBsonValue(string: String): BsonValue {
        return decodeFromString(BsonValueSerializer, string)
    }
}

/**
 * Creates an instance of [Bson] configured from the optionally given [Bson instance][from] and adjusted with [builderAction].
 */
fun Bson(from: Bson = Bson, builderAction: BsonBuilder.() -> Unit): Bson {
    val builder = BsonBuilder(from)
    builder.builderAction()
    val conf = builder.build()
    return BsonImpl(conf, builder.serializersModule)
}

/**
 * Serializes the given [value] into an equivalent [BsonDocument] using a serializer retrieved
 * from reified type parameter.
 *
 * @throws [SerializationException] if the given value cannot be serialized to JSON.
 */
inline fun <reified T> Bson.encodeToBsonDocument(value: T): BsonDocument {
    return encodeToBsonDocument(serializersModule.serializer(), value)
}

/**
 * Deserializes the given [bson] element into a value of type [T] using a deserializer retrieved
 * from reified type parameter.
 *
 * @throws [SerializationException] if the given JSON element is not a valid JSON input for the type [T]
 * @throws [IllegalArgumentException] if the decoded input cannot be represented as a valid instance of type [T]
 */
inline fun <reified T> Bson.decodeFromBsonDocument(bson: BsonDocument): T =
    decodeFromBsonDocument(serializersModule.serializer(), bson)

/**
 * Builder of the [Bson] instance provided by `Bson { ... }` factory function.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class BsonBuilder internal constructor(bson: Bson) {
    /**
     * Specifies whether default values of Kotlin properties should be encoded.
     * `false` by default.
     */
    var encodeDefaults: Boolean = bson.configuration.encodeDefaults

    /**
     * Specifies whether `null` values should be encoded for nullable properties and must be present in JSON object
     * during decoding.
     *
     * When this flag is disabled properties with `null` values without default are not encoded;
     * during decoding, the absence of a field value is treated as `null` for nullable properties without a default value.
     *
     * `true` by default.
     */
    var explicitNulls: Boolean = bson.configuration.explicitNulls

    /**
     * Specifies whether encounters of unknown properties in the input JSON
     * should be ignored instead of throwing [SerializationException].
     * `false` by default.
     */
    var ignoreUnknownKeys: Boolean = bson.configuration.ignoreUnknownKeys

    /**
     * Removes JSON specification restriction (RFC-4627) and makes parser
     * more liberal to the malformed input. In lenient mode quoted boolean literals,
     * and unquoted string literals are allowed.
     *
     * Its relaxations can be expanded in the future, so that lenient parser becomes even more
     * permissive to invalid value in the input, replacing them with defaults.
     *
     * `false` by default.
     */
    var jsonMode: JsonMode = bson.configuration.jsonMode

    /**
     * Enables structured objects to be serialized as map keys by
     * changing serialized form of the map from JSON object (key-value pairs) to flat array like `[k1, v1, k2, v2]`.
     * `false` by default.
     */
    var allowStructuredMapKeys: Boolean = bson.configuration.allowStructuredMapKeys

    /**
     * Specifies whether resulting JSON should be pretty-printed.
     *  `false` by default.
     */
    var prettyPrint: Boolean = bson.configuration.prettyPrint

    /**
     * Specifies indent string to use with [prettyPrint] mode
     * 4 spaces by default.
     * Experimentality note: this API is experimental because
     * it is not clear whether this option has compelling use-cases.
     */
    var prettyPrintIndent: String = bson.configuration.prettyPrintIndent

    /**
     * Enables coercing incorrect JSON values to the default property value in the following cases:
     *   1. JSON value is `null` but property type is non-nullable.
     *   2. Property type is an enum type, but JSON value contains unknown enum member.
     *
     * `false` by default.
     */
    var coerceInputValues: Boolean = bson.configuration.coerceInputValues

    /**
     * Switches polymorphic serialization to the default array format.
     * This is an option for legacy JSON format and should not be generally used.
     * `false` by default.
     */
    var useArrayPolymorphism: Boolean = bson.configuration.useArrayPolymorphism

    /**
     * Name of the class descriptor property for polymorphic serialization.
     * "type" by default.
     */
    var classDiscriminator: String = bson.configuration.classDiscriminator

    var newLineCharacters: String = bson.configuration.newLineCharacters

    /**
     * Module with contextual and polymorphic serializers to be used in the resulting [Bson] instance.
     *
     * @see SerializersModule
     * @see Contextual
     * @see Polymorphic
     */
    var serializersModule: SerializersModule = bson.serializersModule

    @OptIn(ExperimentalSerializationApi::class)
    internal fun build(): BsonConfiguration {
        if (useArrayPolymorphism) require(classDiscriminator == defaultDiscriminator) {
            "Class discriminator should not be specified when array polymorphism is specified"
        }

        if (!prettyPrint) {
            require(prettyPrintIndent == defaultIndent) {
                "Indent should not be specified when default printing mode is used"
            }
        } else if (prettyPrintIndent != defaultIndent) {
            // Values allowed by JSON specification as whitespaces
            val allWhitespaces = prettyPrintIndent.all { it == ' ' || it == '\t' || it == '\r' || it == '\n' }
            require(allWhitespaces) {
                "Only whitespace, tab, newline and carriage return are allowed as pretty print symbols. Had $prettyPrintIndent"
            }
        }

        return BsonConfiguration(
            encodeDefaults = encodeDefaults,
            ignoreUnknownKeys = ignoreUnknownKeys,
            jsonMode = jsonMode,
            newLineCharacters = newLineCharacters,
            allowStructuredMapKeys = allowStructuredMapKeys,
            prettyPrint = prettyPrint,
            explicitNulls = explicitNulls,
            prettyPrintIndent = prettyPrintIndent,
            coerceInputValues = coerceInputValues,
            useArrayPolymorphism = useArrayPolymorphism,
            classDiscriminator = classDiscriminator
        )
    }
}

private class BsonImpl(configuration: BsonConfiguration, module: SerializersModule) : Bson(configuration, module) {

//    init {
//        validateConfiguration()
//    }
//
//    private fun validateConfiguration() {
//        if (serializersModule == EmptySerializersModule()) return // Fast-path for in-place JSON allocations
//        val collector = PolymorphismValidator(configuration.useArrayPolymorphism, configuration.classDiscriminator)
//        serializersModule.dumpTo(collector)
//    }
}

private const val defaultIndent = "  "
private const val defaultDiscriminator = "type"
