package uk.dioxic.helios.execute

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import uk.dioxic.helios.execute.model.State
import uk.dioxic.helios.generate.OperatorFactory
import uk.dioxic.helios.generate.buildTemplate

class StateOperatorTests : FunSpec({

    test("first") {
        OperatorFactory.addOperator(StateOperator::class)

        val mapState = mapOf(
            "name" to "bob",
            "nested" to mapOf(
                "animal" to "badger"
            )
        )

        StateManager.setState(State(mapState))

        buildTemplate {
            put("myName", "\$state.name")
            putJsonObject("myAnimal") {
                put("name", "\$state.nested.animal")
            }
        }.hydrate().should {
            println(it)
            it["myName"] shouldBe "bob"
            it["myAnimal"].should {animal ->
                animal.shouldBeInstanceOf<Map<String,String>>()
                animal["name"] shouldBe "badger"
            }
        }
    }

})