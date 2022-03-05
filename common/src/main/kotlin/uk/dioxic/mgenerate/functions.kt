package uk.dioxic.mgenerate

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.bson.types.ObjectId



interface AnyFunction : () -> Any
interface NumberFunction : () -> Number
interface IntFunction : () -> Int
interface LongFunction : () -> Long
interface StringFunction : () -> String
interface DoubleFunction : () -> Double
interface FloatFunction : () -> Float
interface MapFunction : () -> Map<String, *>
interface ObjectIdFunction : () -> ObjectId
interface ListFunction : () -> List<*>
interface BooleanFunction : () -> Boolean
interface DateTimeFunction : () -> LocalDateTime
interface DateFunction : () -> LocalDate