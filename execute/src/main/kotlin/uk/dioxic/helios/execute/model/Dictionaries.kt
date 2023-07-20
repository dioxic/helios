package uk.dioxic.helios.execute.model

import com.mongodb.MongoNamespace
import com.mongodb.client.MongoClient
import com.mongodb.client.model.Aggregates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.BsonDocument
import uk.dioxic.helios.execute.mongodb.toProjection
import uk.dioxic.helios.execute.resources.ResourceRegistry
import uk.dioxic.helios.execute.serialization.MongoNamespaceSerializer
import uk.dioxic.helios.generate.*

typealias Dictionaries = Map<String, Dictionary>
typealias HydratedDictionary = Map<String, Any?>
typealias HydratedDictionaries = Map<String, HydratedDictionary>

@Serializable
sealed interface Dictionary {
    val store: Store

    context (ResourceRegistry)
    fun asFlow(): Flow<HydratedDictionary>

}

@SerialName("constant")
@Serializable
data class ConstantDictionary(
    val template: Template,
    override val store: Store = Store.NO,
) : Dictionary {

    context (ResourceRegistry)
    override fun asFlow() = flow {
        val hydratedMap = with(OperatorContext.EMPTY) { template.hydrate() }
        while (true) {
            emit(hydratedMap)
        }
    }

}

@SerialName("stream")
@Serializable
data class StreamDictionary(
    val template: Template,
    override val store: Store = Store.NO,
) : Dictionary {

    context (ResourceRegistry)
    override fun asFlow() = flow {
        while (true) {
            emit(template.hydrateAndFlatten())
        }
    }
}

@SerialName("sample")
@Serializable
data class SampleDictionary(
    @SerialName("ns")
    @Serializable(MongoNamespaceSerializer::class) val namespace: MongoNamespace,
    val select: List<String>?,
    val size: Int,
    override val store: Store = Store.NO,
) : Dictionary {

    context (ResourceRegistry)
    override fun asFlow() = flow {
        val collection = getResource<MongoClient>()
            .getDatabase(namespace.databaseName)
            .getCollection(namespace.collectionName)

        val pipeline = buildList {
            add(Aggregates.sample(this@SampleDictionary.size))
            select.toProjection()?.also {
                add(Aggregates.project(it))
            }
        }
        var count: Long

        do {
            count = 0L
            collection.aggregate(pipeline)
                .forEach {
                    emit(it.flatten())
                    count++
                }
        } while (count > 0)
    }
}

@SerialName("query")
@Serializable
data class QueryDictionary(
    @SerialName("ns")
    @Serializable(MongoNamespaceSerializer::class) val namespace: MongoNamespace,
    val filter: Template = Template.EMPTY,
    @Contextual val sort: BsonDocument? = null,
    val select: List<String>? = null,
    override val store: Store = Store.NO,
) : Dictionary {

    context (ResourceRegistry)
    override fun asFlow() = flow {
        val collection = getResource<MongoClient>()
            .getDatabase(namespace.databaseName)
            .getCollection(namespace.collectionName)

        val projection = select.toProjection()
        var count: Long

        do {
            count = 0L
            collection.find(filter)
                .projection(projection)
                .sort(sort)
                .forEach {
                    emit(it.flatten())
                    count++
                }
        } while (count > 0)
    }
}