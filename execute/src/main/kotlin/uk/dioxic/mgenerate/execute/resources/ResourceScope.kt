package uk.dioxic.mgenerate.execute.resources

import arrow.fx.coroutines.ResourceScope
import arrow.fx.coroutines.resourceScope
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients

suspend fun ResourceScope.mongoClient(settings: MongoClientSettings): MongoClient =
    install({ MongoClients.create(settings) }) { client, _ -> client.close() }

suspend fun example() = resourceScope {
    val mcs = MongoClientSettings.builder().build()
    val registry = ResourceRegistry(MongoResource(mongoClient(mcs)))
    val map = mapOf(
        MongoResource::class.java to MongoResource(MongoClients.create())
    )
}