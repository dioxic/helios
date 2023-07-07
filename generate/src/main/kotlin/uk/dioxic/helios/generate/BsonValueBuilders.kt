package uk.dioxic.helios.generate

import kotlinx.serialization.bson.*
import uk.dioxic.helios.generate.OperatorFactory.operatorPrefix
import uk.dioxic.helios.generate.annotations.Alias
import uk.dioxic.helios.generate.operators.RootOperator
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.full.findAnnotations

@OptIn(ExperimentalContracts::class)
inline fun buildTemplate(builderAction: BsonDocumentBuilder.() -> Unit): Template {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    return Bson.decodeFromBsonDocument<Template>(buildBsonDocument(builderAction))
}


/**
 * Add an operator to a BsonArray.
 *
 * Example output:
 * ```
 * [ { "$operator": { ... } } ]
 * ```
 */
inline fun <reified T> BsonArrayBuilder.addOperatorObject(noinline builderAction: BsonDocumentBuilder.() -> Unit): Boolean =
    addBsonDocument {
        putBsonDocument(getOperatorKey<T>(), builderAction)
    }

/**
 * Puts an operator in a BsonDocument.
 *
 * Example output:
 * ```
 * {
 *   "key": {
 *     "$alias": { .. }
 *   }
 * }
 * ```
 * @param key the key in the BsonDocument
 */
fun BsonDocumentBuilder.putOperatorObject(
    key: String,
    alias: String,
    builderAction: BsonDocumentBuilder.() -> Unit
): Boolean =
    putBsonDocument(key) {
        putBsonDocument("$operatorPrefix$alias", builderAction)
    }

/**
 * Puts a keyed operator in a BsonDocument.
 *
 * Example output:
 * ```
 * {
 *   "key": {
 *     "$operator.subKey": { .. }
 *   }
 * }
 * ```
 * @param key the key in the BsonDocument
 * @param subKey the key of the KeyedOperator
 */
inline fun <reified T : KeyedOperator<*>> BsonDocumentBuilder.putKeyedOperatorObject(
    key: String,
    subKey: String,
    noinline builderAction: BsonDocumentBuilder.() -> Unit
): Boolean =
    putBsonDocument(key) {
        putBsonDocument("${getOperatorKey<T>()}.$subKey", builderAction)
    }

/**
 * Puts an operator in a BsonDocument.
 *
 * Example output:
 * ```
 * {
 *   "key": {
 *     "$operator": { .. }
 *   }
 * }
 * ```
 * @param key the key in the BsonDocument
 */
inline fun <reified T> BsonDocumentBuilder.putOperatorObject(
    key: String,
    noinline builderAction: BsonDocumentBuilder.() -> Unit
): Boolean =
    putBsonDocument(key) {
        putBsonDocument(getOperatorKey<T>(), builderAction)
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
 * Add an operator to a BsonArray.
 *
 * Example output:
 * ```
 * [ { "$operator": "value" } ]
 * ```
 * @param value the value to pass to the operator
 */
inline fun <reified T : Operator<*>> BsonArrayBuilder.addOperator(value: String): Boolean =
    addBsonDocument {
        put(getOperatorKey<T>(), value)
    }

/**
 * Add an operator to a BsonArray.
 *
 * Example output:
 * ```
 * [ "$operator" ]
 * ```
 */
inline fun <reified T : Operator<*>> BsonArrayBuilder.addOperator(): Boolean =
    add(getOperatorKey<T>())

/**
 * Puts an operator in a BsonDocument.
 *
 * Example output:
 * ```
 * {
 *   "key": "$operator.subKey"
 * }
 * ```
 * @param key the key in the BsonDocument
 * @param subKey the sub key of the KeyedOperator
 */
inline fun <reified T : Operator<*>> BsonDocumentBuilder.putKeyedOperator(key: String, subKey: String): Boolean =
    put(key, "${getOperatorKey<T>()}.$subKey")

/**
 * Puts an operator in a BsonDocument.
 *
 * Example output:
 * ```
 * {
 *   "key": "$operator"
 * }
 * ```
 * @param key the key in the BsonDocument
 */
inline fun <reified T : Operator<*>> BsonDocumentBuilder.putOperator(key: String): Boolean =
    put(key, getOperatorKey<T>())

/**
 * Puts an operator in a BsonDocument.
 *
 * Example output:
 * ```
 * {
 *   "key": {
 *     "$operator": "value"
 *   }
 * }
 * ```
 * @param key the key in the BsonDocument
 * @param value the value to pass to the Operator
 */
inline fun <reified T : Operator<*>> BsonDocumentBuilder.putOperator(key: String, value: String): Boolean =
    putBsonDocument(key) {
        put(getOperatorKey<T>(), value)
    }

/**
 * Puts an operator in a BsonDocument.
 *
 * Example output:
 * ```
 * {
 *   "$root": "value"
 * }
 * ```
 * @param value the value to pass to the Operator
 */
fun BsonDocumentBuilder.putRootOperator(value: String): Boolean =
    put(getOperatorKey<RootOperator>(), value)

/**
 * Puts an operator in a BsonDocument.
 *
 * Example output:
 * ```
 * {
 *   "key": {
 *     "$operator": "value"
 *   }
 * }
 * ```
 * @param key the key in the BsonDocument
 * @param value the value to pass to the Operator
 */
inline fun <reified T : Operator<*>> BsonDocumentBuilder.putOperator(key: String, value: Number): Boolean =
    putBsonDocument(key) {
        put(getOperatorKey<T>(), value)
    }

/**
 * Puts an operator in a BsonDocument.
 *
 * Example output:
 * ```
 * {
 *   "key": {
 *     "$operator": "value"
 *   }
 * }
 * ```
 * @param key the key in the BsonDocument
 * @param value the value to pass to the Operator
 */
inline fun <reified T : Operator<*>> BsonDocumentBuilder.putOperator(key: String, value: Boolean): Boolean =
    putBsonDocument(key) {
        put(getOperatorKey<T>(), value)
    }

/**
 * Puts an operator in a BsonDocument.
 *
 * Example output:
 * ```
 * {
 *   "key": {
 *     "$operator": "value"
 *   }
 * }
 * ```
 * @param key the key in the BsonDocument
 * @param value the value to pass to the Operator
 */
inline fun <reified T : Operator<*>> BsonDocumentBuilder.putOperator(key: String, value: List<String>): Boolean =
    putBsonDocument(key) {
        putBsonArray(getOperatorKey<T>()) {
            value.forEach {
                add(it)
            }
        }
    }