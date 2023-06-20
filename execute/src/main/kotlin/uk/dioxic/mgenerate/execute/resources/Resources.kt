package uk.dioxic.mgenerate.execute.resources

import arrow.fx.coroutines.ResourceScope
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients

suspend fun ResourceScope.mongoClient(settings: MongoClientSettings): MongoClient =
    install({ MongoClients.create(settings) }) { client, _ -> client.close() }