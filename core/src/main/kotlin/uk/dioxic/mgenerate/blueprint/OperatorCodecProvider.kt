package uk.dioxic.mgenerate.blueprint

import org.bson.*
import org.bson.codecs.BsonTypeClassMap
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.configuration.CodecRegistry

private val bsonTypeClassMap: BsonTypeClassMap = BsonTypeClassMap(
    mapOf(
        BsonType.NULL to BsonNull::class.java,
        BsonType.ARRAY to BsonArray::class.java,
        BsonType.BINARY to BsonBinary::class.java,
        BsonType.BOOLEAN to BsonBoolean::class.java,
        BsonType.DATE_TIME to BsonDateTime::class.java,
        BsonType.DB_POINTER to BsonDbPointer::class.java,
        BsonType.DOCUMENT to BsonDocument::class.java,
        BsonType.DOUBLE to BsonDouble::class.java,
        BsonType.INT32 to BsonInt32::class.java,
        BsonType.INT64 to BsonInt64::class.java,
        BsonType.DECIMAL128 to BsonDecimal128::class.java,
        BsonType.MAX_KEY to BsonMaxKey::class.java,
        BsonType.MIN_KEY to BsonMinKey::class.java,
        BsonType.JAVASCRIPT to BsonJavaScript::class.java,
        BsonType.JAVASCRIPT_WITH_SCOPE to BsonJavaScriptWithScope::class.java,
        BsonType.OBJECT_ID to BsonObjectId::class.java,
        BsonType.REGULAR_EXPRESSION to BsonRegularExpression::class.java,
        BsonType.STRING to BsonString::class.java,
        BsonType.SYMBOL to BsonSymbol::class.java,
        BsonType.TIMESTAMP to BsonTimestamp::class.java,
        BsonType.UNDEFINED to BsonUndefined::class.java
    )
)

@Suppress("UNCHECKED_CAST")
class OperatorCodecProvider : CodecProvider {

    override fun <T : Any> get(clazz: Class<T>, registry: CodecRegistry): Codec<T>? {
        return when(clazz) {
            ChooseOperatorCodec::class.java -> ChooseOperatorCodec(registry) as Codec<T>
            else -> null
        }
    }


}