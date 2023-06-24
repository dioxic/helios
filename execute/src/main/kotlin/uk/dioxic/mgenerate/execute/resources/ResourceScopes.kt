package uk.dioxic.mgenerate.execute.resources

import arrow.fx.coroutines.ResourceScope
import arrow.fx.coroutines.resourceScope
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import uk.dioxic.mgenerate.execute.mongodb.cached

suspend fun ResourceScope.mongoClient(settings: MongoClientSettings): MongoClient =
    install({ MongoClients.create(settings).cached() }) { client, _ -> client.close() }

suspend fun example() = resourceScope {
    val mcs = MongoClientSettings.builder().build()
    val registry = ResourceRegistry(mongoClient(mcs))
}