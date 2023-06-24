package uk.dioxic.mgenerate.execute.mongodb

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase

fun MongoClient.cached() =
    CachedMongoClient(this)

fun MongoDatabase.cached() =
    CachedMongoDatabase(this)