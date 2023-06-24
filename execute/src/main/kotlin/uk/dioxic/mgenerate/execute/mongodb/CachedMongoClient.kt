package uk.dioxic.mgenerate.execute.mongodb

import arrow.core.memoize
import com.mongodb.client.MongoClient

class CachedMongoClient(private val client: MongoClient) : MongoClient by client {
    private val getCachedDatabase = ::databaseFn.memoize()

    private fun databaseFn(databaseName: String) =
        client.getDatabase(databaseName).cached()

    override fun getDatabase(databaseName: String): CachedMongoDatabase =
        getCachedDatabase(databaseName)

}