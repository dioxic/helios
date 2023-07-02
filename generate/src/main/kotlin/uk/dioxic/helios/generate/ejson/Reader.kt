package uk.dioxic.helios.generate.ejson

import org.bson.BsonInvalidOperationException
import org.bson.BsonReader
import org.bson.BsonType

private fun BsonReader.conversionException() =
    BsonInvalidOperationException(
        "Reading field '$currentName' failed, cannot convert $currentBsonType to Int"
    )

fun BsonReader.convertToInt(): Int =
    when (currentBsonType) {
        BsonType.INT32 -> readInt32()
        BsonType.INT64 -> readInt64().toInt()
        BsonType.DOUBLE -> readDouble().toInt()
        else -> throw conversionException()
    }

fun BsonReader.convertToLong(): Long =
    when (currentBsonType) {
        BsonType.INT32 -> readInt32().toLong()
        BsonType.INT64 -> readInt64()
        BsonType.DOUBLE -> readDouble().toLong()
        else -> throw conversionException()
    }

fun BsonReader.convertToDouble(): Double =
    when (currentBsonType) {
        BsonType.INT32 -> readInt32().toDouble()
        BsonType.INT64 -> readInt64().toDouble()
        BsonType.DOUBLE -> readDouble()
        else -> throw conversionException()
    }