package uk.dioxic.mgenerate.execute.resources

import arrow.fx.coroutines.resourceScope
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import kotlin.reflect.KClass

class ResourceRegistry(vararg resource: Resource){
    private val resourceMap = HashMap<KClass<out Resource>, Resource>(resource.associateBy { it::class })

    @Suppress("UNCHECKED_CAST")
    operator fun <T: Resource> get(resourceClass: KClass<T>): T {
        require(resourceMap.contains(resourceClass)) {
            "Resource $resourceClass not found!"
        }
        return resourceMap[resourceClass] as T
    }
}

suspend fun example() = resourceScope {
    val mcs = MongoClientSettings.builder().build()
    val registry = ResourceRegistry(MongoResource(mongoClient(mcs)))
    val map = mapOf(
        MongoResource::class.java to MongoResource(MongoClients.create())
    )
}

sealed interface Resource

class MongoResource(val client: MongoClient) : Resource {

    fun getDatabase(database: String): MongoDatabase =
        client.getDatabase(database)

    inline fun <reified TDocument> getCollection(database: String, collections: String): MongoCollection<TDocument> =
        client.getDatabase(database).getCollection(collections, TDocument::class.java)

}

