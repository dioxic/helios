package uk.dioxic.mgenerate.execute.resources

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase

sealed interface Resource

class MongoResource(val client: MongoClient) : Resource {
    fun getDatabase(database: String): MongoDatabase =
        client.getDatabase(database)

    inline fun <reified TDocument> getCollection(database: String, collections: String): MongoCollection<TDocument> =
        client.getDatabase(database).getCollection(collections, TDocument::class.java)
}