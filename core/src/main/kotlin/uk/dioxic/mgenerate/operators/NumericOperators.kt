package uk.dioxic.mgenerate.operators

import uk.dioxic.mgenerate.annotations.Operator
import kotlin.random.Random

@Operator("long")
fun randomLong(min: Long = 0, max: Long = 10000) =
    Random.nextLong(min, max)

@Operator("double", "dbl")
fun randomDouble(min: Double = 0.0, max: Double = 10000.0) =
    Random.nextDouble(min, max)

@Operator("int")
fun randomInt(min: Int = 0, max: Int = 10000) =
    Random.nextInt(min, max)