package uk.dioxic.helios.generate

import kotlinx.serialization.json.*
import org.bson.BsonBinary
import org.bson.BsonTimestamp
import org.bson.types.ObjectId
import uk.dioxic.helios.generate.OperatorFactory.operatorPrefix
import uk.dioxic.helios.generate.annotations.Alias
import uk.dioxic.helios.generate.codecs.TemplateDocumentCodec
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
    return TemplateDocumentCodec().decode(buildJsonObject(builderAction))
}

inline fun <reified T> JsonArrayBuilder.addOperatorObject(noinline builderAction: JsonObjectBuilder.() -> Unit): Boolean =
    addJsonObject {
        putJsonObject("$operatorPrefix${getOperatorKey<T>()}", builderAction)
    }

fun JsonObjectBuilder.putOperatorObject(
    key: String,
    alias: String,
    builderAction: JsonObjectBuilder.() -> Unit
): JsonElement? =
    putJsonObject(key) {
        putJsonObject("$operatorPrefix$alias", builderAction)
    }

inline fun <reified T> JsonObjectBuilder.putOperatorObject(
    key: String,
    noinline builderAction: JsonObjectBuilder.() -> Unit
): JsonElement? =
    putJsonObject(key) {
        putJsonObject("$operatorPrefix${getOperatorKey<T>()}", builderAction)
    }

inline fun <reified T> getOperatorKey(): String {
    val alias = T::class.findAnnotations(Alias::class).firstOrNull()
    return if (alias != null && alias.aliases.isNotEmpty()) {
        alias.aliases.first()
    } else {
        T::class.simpleName!!
    }
}

inline fun <reified T : Operator<*>> JsonArrayBuilder.addOperator(value: String): Boolean =
    addJsonObject {
        put(getOperatorKey<T>(), value)
    }

inline fun <reified T : Operator<*>> JsonArrayBuilder.addOperator(): Boolean =
    add(getOperatorKey<T>())

inline fun <reified T : Operator<*>> JsonObjectBuilder.putOperator(key: String): JsonElement? =
    put(key, getOperatorKey<T>())

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