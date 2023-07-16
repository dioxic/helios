@file:Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")

package uk.dioxic.helios.execute.model

import arrow.fx.coroutines.resourceScope
import com.mongodb.TransactionOptions
import com.mongodb.client.ClientSession
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import uk.dioxic.helios.execute.mongodb.withTransaction
import uk.dioxic.helios.execute.resources.ResourceRegistry
import uk.dioxic.helios.execute.resources.mongoSession
import uk.dioxic.helios.execute.results.*
import uk.dioxic.helios.execute.serialization.TransactionOptionsSerializer
import uk.dioxic.helios.generate.Template
import uk.dioxic.helios.generate.buildTemplate
import uk.dioxic.helios.generate.hydrate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Serializable
sealed interface Executor {
    context(ExecutionContext, ResourceRegistry)
    suspend fun execute(): ExecutionResult
}

sealed interface MongoSessionExecutor : Executor {
    context(ExecutionContext, ResourceRegistry)
    suspend fun execute(session: ClientSession): ExecutionResult
}

sealed interface WriteModelExecutor : Executor {
    suspend fun writeModel(): List<WriteModel<Template>>
}

@Serializable
sealed class CollectionExecutor : DatabaseExecutor(), MongoSessionExecutor {
    abstract override val database: String
    abstract val collection: String

    context(ResourceRegistry)
    inline fun <reified TDocument> getCollection(): MongoCollection<TDocument> =
        getResource<MongoClient>().getDatabase(database).getCollection(collection, TDocument::class.java)

}

@Serializable
sealed class DatabaseExecutor : MongoSessionExecutor {
    abstract val database: String

    context(ResourceRegistry)
    fun getDatabase(): MongoDatabase = getResource<MongoClient>().getDatabase(database)
}

@Serializable
@SerialName("message")
class MessageExecutor(
    val template: Template
) : Executor {

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute() =
        MessageResult(template.hydrate())

}

@Serializable
@SerialName("drop")
class DropExecutor(
    override val database: String,
    val collection: String?,
) : DatabaseExecutor() {

    val dropCommand = if (collection != null) {
        CommandExecutor(database, buildTemplate {
            put("drop", collection)
        })
    } else {
        CommandExecutor(database, buildTemplate {
            put("dropDatabase", 1)
        })
    }

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute(session: ClientSession): CommandResult =
        dropCommand.execute(session)

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute() =
        dropCommand.execute()

}

@Serializable
@SerialName("command")
class CommandExecutor(
    override val database: String,
    val command: Template
) : DatabaseExecutor() {
    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute(session: ClientSession) = CommandResult(
        getDatabase().runCommand(session, command)
    )

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute() = CommandResult(
        getDatabase().runCommand(command)
    )
}

@Serializable
@SerialName("insertOne")
class InsertOneExecutor(
    override val database: String,
    override val collection: String,
    val template: Template
) : CollectionExecutor(), WriteModelExecutor {

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute(session: ClientSession): ExecutionResult =
        getCollection<Template>().insertOne(session, template).standardize()


    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute() =
        getCollection<Template>().insertOne(template).standardize()

    override suspend fun writeModel() =
        listOf(InsertOneModel(template))
}

@Serializable
@SerialName("insertMany")
class InsertManyExecutor(
    override val database: String,
    override val collection: String,
    val template: Template,
    val size: Int = 1,
    private val ordered: Boolean = true,
) : CollectionExecutor(), WriteModelExecutor {

    @Transient
    private val options = InsertManyOptions().ordered(ordered)

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute(session: ClientSession) =
        getCollection<Template>().insertMany(session, List(size) { template }, options).standardize()

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute() =
        getCollection<Template>().insertMany(List(size) { template }, options).standardize()

    override suspend fun writeModel() =
        List(size) { InsertOneModel(template) }
}

@Serializable
@SerialName("deleteOne")
class DeleteOneExecutor(
    override val database: String,
    override val collection: String,
    val filter: Template,
) : CollectionExecutor(), WriteModelExecutor {

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute(session: ClientSession) =
        getCollection<Template>().deleteOne(session, filter).standardize()

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute() =
        getCollection<Template>().deleteOne(filter).standardize()

    override suspend fun writeModel() =
        listOf(DeleteOneModel<Template>(filter))
}

@Serializable
@SerialName("deleteMany")
class DeleteManyExecutor(
    override val database: String,
    override val collection: String,
    val filter: Template,
) : CollectionExecutor(), WriteModelExecutor {

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute(session: ClientSession) =
        getCollection<Template>().deleteMany(session, filter).standardize()

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute() =
        getCollection<Template>().deleteMany(filter).standardize()

    override suspend fun writeModel() =
        listOf(DeleteManyModel<Template>(filter))
}

@Serializable
@SerialName("replaceOne")
class ReplaceOneExecutor(
    override val database: String,
    override val collection: String,
    val filter: Template,
    val replacement: Template,
    @Contextual val options: ReplaceOptions
) : CollectionExecutor(), WriteModelExecutor {

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute(session: ClientSession) =
        getCollection<Template>().replaceOne(session, filter, replacement, options).standardize()

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute() =
        getCollection<Template>().replaceOne(filter, replacement, options).standardize()

    override suspend fun writeModel() =
        listOf(ReplaceOneModel<Template>(filter, replacement, options))
}

@Serializable
@SerialName("updateOne")
class UpdateOneExecutor(
    override val database: String,
    override val collection: String,
    val filter: Template,
    val update: Template,
    @Contextual val options: UpdateOptions
) : CollectionExecutor(), WriteModelExecutor {

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute(session: ClientSession) =
        getCollection<Template>().updateOne(session, filter, update, options).standardize()

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute() =
        getCollection<Template>().updateOne(filter, update, options).standardize()

    override suspend fun writeModel() =
        listOf(UpdateOneModel<Template>(filter, update, options))
}

@Serializable
@SerialName("updateMany")
class UpdateManyExecutor(
    override val database: String,
    override val collection: String,
    val filter: Template,
    val update: Template,
    @Contextual val options: UpdateOptions
) : CollectionExecutor(), WriteModelExecutor {

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute(session: ClientSession) =
        getCollection<Template>().updateMany(session, filter, update, options).standardize()

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute() =
        getCollection<Template>().updateMany(filter, update, options).standardize()

    override suspend fun writeModel() =
        listOf(UpdateManyModel<Template>(filter, update, options))
}

@Serializable
@SerialName("bulk")
class BulkWriteExecutor(
    override val database: String,
    override val collection: String,
    val operations: List<WriteOperation>
) : CollectionExecutor() {

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute(): ExecutionResult = createCache().let { cache ->
        getCollection<EncodeContext>().bulkWrite(operations.flatMap {
            it.getWriteModels(cache)
        }).standardize()
    }

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute(session: ClientSession): ExecutionResult = createCache().let { cache ->
        getCollection<EncodeContext>().bulkWrite(session, operations.flatMap {
            it.getWriteModels(cache)
        }).standardize()
    }

    context(ExecutionContext)
    private fun createCache(): VariablesCache {
        val maxModels = operations.maxOfOrNull { it.count } ?: 100
        return List(maxModels) {
            lazy(LazyThreadSafetyMode.NONE) {
                variables.value + workload.variables
            }
        }
    }

}

@Serializable
@SerialName("transaction")
class TransactionExecutor(
    val executors: List<MongoSessionExecutor>,
    @Serializable(TransactionOptionsSerializer::class) val options: TransactionOptions,
    val maxRetryTimeout: Duration = 120.seconds,
    val maxRetryAttempts: Int = 100,
) : Executor {

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute(): ExecutionResult =
        resourceScope {
            val session = mongoSession(getResource<MongoClient>())
//            val txnBody = TransactionBody {
//                runBlocking {
//                    TransactionResult(executors.map { it.execute(session) })
//                }
//            }
//            val results = session.withTransaction(txnBody, options)
            session.withTransaction(options, maxRetryTimeout, maxRetryAttempts) {
                TransactionResult(executors.map { it.execute(session) })
            }
        }

}