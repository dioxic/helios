package uk.dioxic.helios.execute.resources

import arrow.fx.coroutines.ResourceScope
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import okio.FileSystem

//class DictionaryResource private constructor(
//    val sink: BufferedSink?,
//    val source: BufferedSource?
//) {
//    constructor(sink: BufferedSink) : this(sink, null)
//    constructor(source: BufferedSource) : this(null, source)
//
//    fun isSink() = sink != null
//    fun isSource() = source != null
//}

interface ResourceRegistry {
//    val dictionaryStores: Map<String, DictionaryResource>
    val mongoClient: MongoClient

    companion object {
        val EMPTY: ResourceRegistry = ResourceRegistryImpl()
    }
}

private class ResourceRegistryImpl(
    mongoClient: MongoClient? = null,
//    override val dictionaryStores: Map<String, DictionaryResource> = emptyMap()
) : ResourceRegistry {
    private val _mongoClient = mongoClient

    override val mongoClient: MongoClient
        get() {
            requireNotNull(_mongoClient) {
                "Mongo client not set"
            }
            return _mongoClient
        }
}

suspend fun buildResourceRegistry(
    fileSystem: FileSystem = FileSystem.SYSTEM,
    init: suspend ResourceRegistryBuilder.() -> Unit
): ResourceRegistry {
    val builder = ResourceRegistryBuilder(fileSystem)
    init.invoke(builder)
    return builder.build()
}

@Suppress("MemberVisibilityCanBePrivate")
class ResourceRegistryBuilder(val fileSystem: FileSystem) {
//    private val dictionaryStore = mutableMapOf<String, DictionaryResource>()

    var mongoClient: MongoClient? = null

    context(ResourceScope)
    suspend fun createMongoClient(mongoClientSettings: MongoClientSettings) {
        mongoClient = mongoClient(mongoClientSettings)
    }

//    context(ResourceScope)
//    suspend fun addDictionaries(dictionaries: Dictionaries) {
//        dictionaries.forEach { (k, v) ->
//            addDictionary(k, v)
//        }
//    }

//    context(ResourceScope)
//    suspend fun addDictionary(key: String, dictionary: Dictionary) {
//        val store = dictionary.store
//
//        if (store != Store.NO) {
//            val file = File(
//                if (store is PathStore) {
//                    store.path
//                } else {
//                    "$key.json"
//                }
//            )
//
//            dictionaryStore[key] = if (file.exists()) {
//                require(!file.isDirectory) {
//                    "Store [$key] cannot be a directory"
//                }
//                DictionaryResource(fileSource(fileSystem, file))
//            } else {
//                DictionaryResource(fileSink(fileSystem, file))
//            }
//        }
//    }

    fun build(): ResourceRegistry =
        ResourceRegistryImpl(mongoClient)
}