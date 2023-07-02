package uk.dioxic.helios.generate

import kotlinx.serialization.json.*
import org.bson.BsonBinary
import org.bson.BsonTimestamp
import org.bson.types.ObjectId
import uk.dioxic.helios.generate.OperatorFactory.operatorPrefix
import uk.dioxic.helios.generate.annotations.Alias
import uk.dioxic.helios.generate.codecs.TemplateCodec
import uk.dioxic.helios.generate.operators.RootOperator
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.full.findAnnotations

@OptIn(ExperimentalContracts::class)
inline fun buildTemplate(builderAction: JsonObjectBuilder.() -> Unit): Template {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    return TemplateCodec().decode(buildJsonObject(builderAction))
}

/**
 * Add an operator to a JsonArray.
 *
 * Example output:
 * ```
 * [ { "$operator": { ... } } ]
 * ```
 * @param value the value to pass to the operator
 */
inline fun <reified T> JsonArrayBuilder.addOperatorObject(noinline builderAction: JsonObjectBuilder.() -> Unit): Boolean =
    addJsonObject {
        putJsonObject(getOperatorKey<T>(), builderAction)
    }

/**
 * Puts an operator in a JsonObject.
 *
 * Example output:
 * ```
 * {
 *   "key": {
 *     "$alias": { .. }
 *   }
 * }
 * ```
 * @param key the key in the JsonObject
 */
fun JsonObjectBuilder.putOperatorObject(
    key: String,
    alias: String,
    builderAction: JsonObjectBuilder.() -> Unit
): JsonElement? =
    putJsonObject(key) {
        putJsonObject("$operatorPrefix$alias", builderAction)
    }

/**
 * Puts a keyed operator in a JsonObject.
 *
 * Example output:
 * ```
 * {
 *   "key": {
 *     "$operator.subKey": { .. }
 *   }
 * }
 * ```
 * @param key the key in the JsonObject
 * @param subKey the key of the KeyedOperator
 */
inline fun <reified T : KeyedOperator<*>> JsonObjectBuilder.putKeyedOperatorObject(
    key: String,
    subKey: String,
    noinline builderAction: JsonObjectBuilder.() -> Unit
): JsonElement? =
    putJsonObject(key) {
        putJsonObject("${getOperatorKey<T>()}.$subKey", builderAction)
    }

/**
 * Puts an operator in a JsonObject.
 *
 * Example output:
 * ```
 * {
 *   "key": {
 *     "$operator": { .. }
 *   }
 * }
 * ```
 * @param key the key in the JsonObject
 */
inline fun <reified T> JsonObjectBuilder.putOperatorObject(
    key: String,
    noinline builderAction: JsonObjectBuilder.() -> Unit
): JsonElement? =
    putJsonObject(key) {
        putJsonObject(getOperatorKey<T>(), builderAction)
    }

inline fun <reified T> getOperatorKey(): String {
    val alias = T::class.findAnnotations(Alias::class).firstOrNull()
    val key = if (alias != null && alias.aliases.isNotEmpty()) {
        alias.aliases.first()
    } else {
        T::class.simpleName!!
    }
    return "$operatorPrefix$key"
}

inline fun <reified T> getOperatorKey(subkey: String): String =
    "${getOperatorKey<T>()}.$subkey"

/**
 * Add an operator to a JsonArray.
 *
 * Example output:
 * ```
 * [ { "$operator": "value" } ]
 * ```
 * @param value the value to pass to the operator
 */
inline fun <reified T : Operator<*>> JsonArrayBuilder.addOperator(value: String): Boolean =
    addJsonObject {
        put(getOperatorKey<T>(), value)
    }

/**
 * Add an operator to a JsonArray.
 *
 * Example output:
 * ```
 * [ "$operator" ]
 * ```
 */
inline fun <reified T : Operator<*>> JsonArrayBuilder.addOperator(): Boolean =
    add(getOperatorKey<T>())

/**
 * Puts an operator in a JsonObject.
 *
 * Example output:
 * ```
 * {
 *   "key": "$operator.subKey"
 * }
 * ```
 * @param key the key in the JsonObject
 * @param subKey the sub key of the KeyedOperator
 */
inline fun <reified T : Operator<*>> JsonObjectBuilder.putKeyedOperator(key: String, subKey: String): JsonElement? =
    put(key, "${getOperatorKey<T>()}.$subKey")

/**
 * Puts an operator in a JsonObject.
 *
 * Example output:
 * ```
 * {
 *   "key": "$operator"
 * }
 * ```
 * @param key the key in the JsonObject
 */
inline fun <reified T : Operator<*>> JsonObjectBuilder.putOperator(key: String): JsonElement? =
    put(key, getOperatorKey<T>())

/**
 * Puts an operator in a JsonObject.
 *
 * Example output:
 * ```
 * {
 *   "key": {
 *     "$operator": "value"
 *   }
 * }
 * ```
 * @param key the key in the JsonObject
 * @param value the value to pass to the Operator
 */
inline fun <reified T : Operator<*>> JsonObjectBuilder.putOperator(key: String, value: String): JsonElement? =
    putJsonObject(key) {
        put(getOperatorKey<T>(), value)
    }

/**
 * Puts an operator in a JsonObject.
 *
 * Example output:
 * ```
 * {
 *   "$root": "value"
 * }
 * ```
 * @param value the value to pass to the Operator
 */
fun JsonObjectBuilder.putRootOperator(value: String): JsonElement? =
    put(getOperatorKey<RootOperator>(), value)

/**
 * Puts an operator in a JsonObject.
 *
 * Example output:
 * ```
 * {
 *   "key": {
 *     "$operator": "value"
 *   }
 * }
 * ```
 * @param key the key in the JsonObject
 * @param value the value to pass to the Operator
 */
inline fun <reified T : Operator<*>> JsonObjectBuilder.putOperator(key: String, value: Number): JsonElement? =
    putJsonObject(key) {
        put(getOperatorKey<T>(), value)
    }

/**
 * Puts an operator in a JsonObject.
 *
 * Example output:
 * ```
 * {
 *   "key": {
 *     "$operator": "value"
 *   }
 * }
 * ```
 * @param key the key in the JsonObject
 * @param value the value to pass to the Operator
 */
inline fun <reified T : Operator<*>> JsonObjectBuilder.putOperator(key: String, value: Boolean): JsonElement? =
    putJsonObject(key) {
        put(getOperatorKey<T>(), value)
    }

/**
 * Puts an operator in a JsonObject.
 *
 * Example output:
 * ```
 * {
 *   "key": {
 *     "$operator": "value"
 *   }
 * }
 * ```
 * @param key the key in the JsonObject
 * @param value the value to pass to the Operator
 */
inline fun <reified T : Operator<*>> JsonObjectBuilder.putOperator(key: String, value: List<String>): JsonElement? =
    putJsonObject(key) {
        putJsonArray(getOperatorKey<T>()) {
            value.forEach {
                add(it)
            }
        }
    }

fun JsonObjectBuilder.put(key: String, value: LocalDateTime): JsonElement? =
    put(key, value.toInstant(ZoneOffset.UTC))

fun JsonObjectBuilder.put(key: String, value: Instant): JsonElement? =
    putJsonObject(key) {
        putJsonObject("\$date") {
            put("\$numberLong", value.toEpochMilli().toString())
        }
    }

fun JsonObjectBuilder.put(key: String, value: ObjectId): JsonElement? =
    putJsonObject(key) {
        put("\$oid", value.toHexString())
    }

fun JsonObjectBuilder.put(key: String, value: BsonTimestamp): JsonElement? =
    putJsonObject(key) {
        putJsonObject("\$timestamp") {
            put("t", value.time)
            put("i", value.inc)
        }
    }

fun JsonObjectBuilder.put(key: String, value: BsonBinary): JsonElement? =
    putJsonObject(key) {
        putJsonObject("\$binary") {
            put("base64", Base64.getEncoder().encodeToString(value.data))
            put("subType", String.format("%02X", value.type))
        }
    }

fun JsonObjectBuilder.put(key: String, value: UUID): JsonElement? =
    put(key, BsonBinary(value))