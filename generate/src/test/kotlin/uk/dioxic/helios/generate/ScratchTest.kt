package uk.dioxic.helios.generate

import arrow.core.Either
import arrow.core.Nel
import arrow.core.mapOrAccumulate
import arrow.core.raise.Raise
import arrow.core.raise.catch
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.put
import org.bson.Document
import uk.dioxic.helios.generate.exceptions.ConversionError
import uk.dioxic.helios.generate.operators.ArrayOperator
import uk.dioxic.helios.generate.operators.IntOperator
import uk.dioxic.helios.generate.operators.NameOperator
import uk.dioxic.helios.generate.operators.fakerGenerators
import uk.dioxic.helios.generate.test.withEmptyContext


fun opBuild(l: List<String>): Either<Nel<ConversionError>, List<Int>> {
    return l.mapOrAccumulate { convert(it) }
}

context(Raise<ConversionError>)
fun convert(s: String): Int {
    return catch({ s.toInt() }) { raise(ConversionError(s, Int::class)) }

//    return when(s) {
//        "1" -> s.toInt()
//        else -> raise(ConversionError(s))
//    }
}


private val json = Json { prettyPrint = true }

class ScratchTest : FunSpec({

    test("errors") {
        opBuild(listOf("1", "2", "x", "y", "z")).shouldBeLeft().should {
            it.forEach(::println)
        }

        opBuild(listOf("1", "2", "3", "4", "5")).shouldBeRight().should {
            println(it)
        }

    }

    test("misc") {
        val txt = withEmptyContext {
            fakerGenerators.keys.first().invoke()
        }

        println(txt)
    }

    test("single nested operator hydrates correctly") {
        val template = buildTemplate {
            putOperator<NameOperator>("name")
            putOperatorObject<ArrayOperator>("animals") {
                putOperatorObject<IntOperator>("of") {
                    put("min", 10)
                    put("max", 20)
                }
            }
            putOperatorObject<IntOperator>("age") {
                put("min", 10)
                put("max", 13)
            }
        }

        with(OperatorContext.EMPTY) {
            println(json.encodeToString(template))
            val hDoc = hydrate(template)
            println(Document(hDoc as Map<String, Any?>).toJson())
        }
    }

})