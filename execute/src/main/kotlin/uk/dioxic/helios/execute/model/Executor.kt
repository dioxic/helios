package uk.dioxic.helios.execute.model

import arrow.fx.coroutines.resourceScope
import com.mongodb.TransactionOptions
import com.mongodb.client.ClientSession
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import uk.dioxic.helios.execute.mongodb.withTransaction
import uk.dioxic.helios.execute.resources.ResourceRegistry
import uk.dioxic.helios.execute.resources.mongoSession
import uk.dioxic.helios.execute.results.*
import uk.dioxic.helios.execute.serialization.TransactionOptionsSerializer
import uk.dioxic.helios.generate.Template
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

@Serializable
sealed class CollectionExecutor : Executor {
    abstract val database: String
    abstract val collection: String

    context(ResourceRegistry)
    inline fun <reified TDocument> getCollection(): MongoCollection<TDocument> =
        getResource<MongoClient>().getDatabase(database).getCollection(collection, TDocument::class.java)

}

@Serializable
sealed class DatabaseExecutor : Executor {
    abstract val database: String

    context(ResourceRegistry)
    fun getDatabase(): MongoDatabase = getResource<MongoClient>().getDatabase(database)
}

@Serializable
@SerialName("message")
data class MessageExecutor(
    val message: String
) : Executor {

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute() =
        MessageResult("$message - count: $executionCount")

}

@Serializable
@SerialName("command")
data class CommandExecutor(
    override val database: String,
    val command: Template
) : DatabaseExecutor() {

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute() = CommandResult(
        getDatabase().runCommand(command)
    )
}

@Serializable
@SerialName("insertOne")
data class InsertOneExecutor(
    override val database: String,
    override val collection: String,
    val template: Template
) : CollectionExecutor(), MongoSessionExecutor {

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute(session: ClientSession): ExecutionResult =
        getCollection<Template>().insertOne(session, template).standardize()

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute() =
        getCollection<Template>().insertOne(template).standardize()

}

@Serializable
@SerialName("transaction")
data class TransactionExecutor(
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