package uk.dioxic.helios.execute

import arrow.fx.coroutines.resourceScope
import com.mongodb.MongoClientSettings
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.resource.resourceAsString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.take
import kotlinx.serialization.bson.Bson
import kotlinx.serialization.decodeFromString
import uk.dioxic.helios.execute.model.Dictionaries
import uk.dioxic.helios.execute.model.HydratedDictionary
import uk.dioxic.helios.execute.resources.ResourceRegistry
import uk.dioxic.helios.execute.resources.mongoClient

typealias DictionaryFlows = Map<String, Flow<HydratedDictionary>>
typealias DictionaryFlow = Flow<Map<String, HydratedDictionary>>
typealias DictionaryPair = Flow<Pair<String, HydratedDictionary>>

class DictionaryTests : FunSpec({

    test("deserialization") {
        resourceScope {
            val mcs = MongoClientSettings.builder().build()
            val client = mongoClient(mcs)
            with(ResourceRegistry(client)) {
                val str = resourceAsString("/dictionaries.json")

                val dictionaries = Bson.decodeFromString<Dictionaries>(str)

                println(dictionaries)

                dictionaries.asFlow().take(10).collect {
                    println(it)
                }

//                dictionaries["person"]?.also { dictionary ->
//                    dictionary.asFlow().take(10).collect {
//                        println(it)
//                    }
//                }
            }
        }
    }

})