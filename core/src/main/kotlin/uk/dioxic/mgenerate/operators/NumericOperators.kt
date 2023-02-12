package uk.dioxic.mgenerate.operators

import uk.dioxic.mgenerate.annotations.Alias
import kotlin.random.Random

@Alias("long")
fun randomLong(min: Long = 0, max: Long = 10000) =
    Random.nextLong(min, max)

@Alias("double", "dbl")
fun randomDouble(min: Double = 0.0, max: Double = 10000.0) =
    Random.nextDouble(min, max)

@Alias("int")
fun randomInt(min: Int = 0, max: Int = 10000) =
    Random.nextInt(min, max)