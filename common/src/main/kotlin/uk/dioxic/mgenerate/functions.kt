package uk.dioxic.mgenerate

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.bson.types.ObjectId


interface TypedFunction
interface AnyFunction : () -> Any, TypedFunction
interface NumberFunction : () -> Number, TypedFunction
interface IntFunction : () -> Int, TypedFunction
interface LongFunction : () -> Long, TypedFunction
interface StringFunction : () -> String, TypedFunction
interface DoubleFunction : () -> Double, TypedFunction
interface FloatFunction : () -> Float, TypedFunction
interface MapFunction : () -> Map<String, *>, TypedFunction
interface ObjectIdFunction : () -> ObjectId, TypedFunction
interface ListFunction : () -> List<*>, TypedFunction
interface BooleanFunction : () -> Boolean, TypedFunction
interface DateTimeFunction : () -> LocalDateTime, TypedFunction
interface DateFunction : () -> LocalDate, TypedFunction