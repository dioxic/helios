package uk.dioxic.mgenerate.execute.resources

import arrow.core.continuations.AtomicRef
import arrow.core.continuations.loop
import arrow.core.memoize
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase

sealed interface Resource

class MongoResource(private val client: MongoClient) : Resource {
    private val cache = CollectionCache(client)

    private fun getDatabaseInternal(database: String): MongoDatabase =
        client.getDatabase(database)

    val getDatabase = ::getDatabaseInternal.memoize()

    fun <TDocument> getCollection(
        database: String,
        collections: String,
        documentClass: Class<TDocument>
    ): MongoCollection<TDocument> =
        cache[database, collections, documentClass]

    inline fun <reified TDocument> getCollection(database: String, collections: String): MongoCollection<TDocument> =
        getCollection(database, collections, TDocument::class.java)
}

class CollectionCache(private val client: MongoClient) {
    private val cache = AtomicRef(emptyMap<Triple<String, String, Class<*>>, MongoCollection<*>>())

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(database: String, collection: String, documentClass: Class<T>): MongoCollection<T> =
        when (val k = Triple(database, collection, documentClass)) {
            in cache.get() -> cache.get().getValue(k) as MongoCollection<T>
            else -> {
                val b = client.getDatabase(database).getCollection(collection, documentClass)
                cache.loop { old ->
                    when (k) {
                        in old ->
                            return old.getValue(k) as MongoCollection<T>
                        else -> {
                            if (cache.compareAndSet(old, old + Pair(k, b))) {
                                return b
                            }
                        }
                    }
                }
            }
        }
}