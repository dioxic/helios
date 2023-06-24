package uk.dioxic.mgenerate.execute.mongodb

import arrow.core.memoize
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document

class CachedMongoDatabase(private val mongoDatabase: MongoDatabase) : MongoDatabase by mongoDatabase {
    private val getCachedCollection = ::collectionFn.memoize()

    private fun collectionFn(
        collectionName: String,
        documentClass: Class<*>
    ): MongoCollection<*> =
        mongoDatabase.getCollection(collectionName, documentClass)

    override fun getCollection(collectionName: String): MongoCollection<Document> =
        getCollection(collectionName, Document::class.java)

    @Suppress("UNCHECKED_CAST")
    override fun <TDocument> getCollection(
        collectionName: String,
        documentClass: Class<TDocument>
    ): MongoCollection<TDocument> =
        getCachedCollection(collectionName, documentClass) as MongoCollection<TDocument>

    inline fun <reified TDocument> getCollectionK(collectionName: String): MongoCollection<TDocument> =
        getCollection(collectionName, TDocument::class.java)
}