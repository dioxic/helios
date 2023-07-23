@file:Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")

package uk.dioxic.helios.execute.model

import arrow.fx.coroutines.resourceScope
import com.mongodb.TransactionOptions
import com.mongodb.client.ClientSession
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.InsertManyOptions
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.UpdateOptions
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import uk.dioxic.helios.execute.mongodb.withTransaction
import uk.dioxic.helios.execute.resources.ResourceRegistry
import uk.dioxic.helios.execute.resources.mongoSession
import uk.dioxic.helios.execute.results.*
import uk.dioxic.helios.execute.serialization.TransactionOptionsSerializer
import uk.dioxic.helios.generate.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Serializable
sealed interface Executor {
    @Transient
    val variablesRequired: Int

    context(ExecutionContext, ResourceRegistry)
    suspend fun execute(): ExecutionResult

}

sealed interface MongoSessionExecutor : Executor {
    context(ExecutionContext, ResourceRegistry)
    suspend fun execute(session: ClientSession): ExecutionResult
}

sealed interface SingleVariableExecutor : Executor {
    @Transient
    override val variablesRequired
        get() = 1

    fun Template.toEncodeContext(stateContext: List<StateContext>) =
        EncodeContext(this, stateContext.first())
}

sealed interface MultiVariableExecutor : Executor {
    fun Template.toEncodeContext(stateContexts: List<StateContext>) = List(variablesRequired) {
        EncodeContext(this, stateContexts[it])
    }
}

@Serializable
sealed class CollectionExecutor : DatabaseExecutor(), MongoSessionExecutor {
    abstract override val database: String
    abstract val collection: String

    context(ResourceRegistry)
    inline fun <reified TDocument> getCollection(): MongoCollection<TDocument> =
        mongoClient.getDatabase(database).getCollection(collection, TDocument::class.java)

}

@Serializable
sealed class DatabaseExecutor : MongoSessionExecutor {
    abstract val database: String

    context(ResourceRegistry)
    fun getDatabase(): MongoDatabase = mongoClient.getDatabase(database)
}

@Serializable
@SerialName("message")
class MessageExecutor(
    val template: Template
) : SingleVariableExecutor {

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute(): MessageResult =
        with(stateContext.first()) {
            MessageResult(template.hydrate())
        }

}

@Serializable
@SerialName("drop")
class DropExecutor(
    override val database: String,
    val collection: String?,
) : DatabaseExecutor(), SingleVariableExecutor {

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
) : DatabaseExecutor(), SingleVariableExecutor {
    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute(session: ClientSession): CommandResult {
        OperatorContext.threadLocal.set(stateContext.first())
        return CommandResult(
            getDatabase().runCommand(session, command.toEncodeContext(stateContext))
        )
    }

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute(): CommandResult {
        OperatorContext.threadLocal.set(stateContext.first())
        return CommandResult(
            getDatabase().runCommand(command)
        )
    }
}

@Serializable
@SerialName("insertOne")
class InsertOneExecutor(
    override val database: String,
    override val collection: String,
    val template: Template
) : CollectionExecutor(), SingleVariableExecutor {

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute(session: ClientSession): ExecutionResult =
        getCollection<EncodeContext>().insertOne(session, template.toEncodeContext(stateContext)).standardize()


    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute() =
        getCollection<EncodeContext>().insertOne(template.toEncodeContext(stateContext)).standardize()

}

@Serializable
@SerialName("insertMany")
class InsertManyExecutor(
    override val database: String,
    override val collection: String,
    val template: Template,
    val size: Int = 1,
    private val ordered: Boolean = true,
) : CollectionExecutor(), MultiVariableExecutor {

    @Transient
    private val options = InsertManyOptions().ordered(ordered)

    @Transient
    override val variablesRequired = size

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute(session: ClientSession) =
        getCollection<EncodeContext>().insertMany(
            session,
            template.toEncodeContext(stateContext),
            options
        ).standardize()

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute() =
        getCollection<EncodeContext>().insertMany(template.toEncodeContext(stateContext), options).standardize()

}

@Serializable
@SerialName("deleteOne")
class DeleteOneExecutor(
    override val database: String,
    override val collection: String,
    val filter: Template,
) : CollectionExecutor(), SingleVariableExecutor {

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute(session: ClientSession) =
        getCollection<EncodeContext>().deleteOne(session, filter.toEncodeContext(stateContext)).standardize()

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute() =
        getCollection<EncodeContext>().deleteOne(filter.toEncodeContext(stateContext)).standardize()

}

@Serializable
@SerialName("deleteMany")
class DeleteManyExecutor(
    override val database: String,
    override val collection: String,
    val filter: Template,
) : CollectionExecutor(), SingleVariableExecutor {

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute(session: ClientSession) =
        getCollection<EncodeContext>().deleteMany(session, filter.toEncodeContext(stateContext)).standardize()

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute() =
        getCollection<EncodeContext>().deleteMany(filter.toEncodeContext(stateContext)).standardize()

}

@Serializable
@SerialName("replaceOne")
class ReplaceOneExecutor(
    override val database: String,
    override val collection: String,
    val filter: Template,
    val replacement: Template,
    @Contextual val options: ReplaceOptions
) : CollectionExecutor(), SingleVariableExecutor {

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute(session: ClientSession) =
        getCollection<EncodeContext>().replaceOne(
            session,
            filter.toEncodeContext(stateContext),
            replacement.toEncodeContext(stateContext),
            options
        ).standardize()

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute() =
        getCollection<EncodeContext>().replaceOne(
            filter.toEncodeContext(stateContext),
            replacement.toEncodeContext(stateContext),
            options
        ).standardize()

}

@Serializable
@SerialName("updateOne")
class UpdateOneExecutor(
    override val database: String,
    override val collection: String,
    val filter: Template,
    val update: Template,
    @Contextual val options: UpdateOptions
) : CollectionExecutor(), SingleVariableExecutor {

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute(session: ClientSession) =
        getCollection<EncodeContext>().updateOne(
            session,
            filter.toEncodeContext(stateContext),
            update.toEncodeContext(stateContext),
            options
        ).standardize()

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute() =
        getCollection<EncodeContext>().updateOne(
            filter.toEncodeContext(stateContext),
            update.toEncodeContext(stateContext),
            options
        ).standardize()

}

@Serializable
@SerialName("updateMany")
class UpdateManyExecutor(
    override val database: String,
    override val collection: String,
    val filter: Template,
    val update: Template,
    @Contextual val options: UpdateOptions
) : CollectionExecutor(), SingleVariableExecutor {

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute(session: ClientSession) =
        getCollection<Template>().updateMany(
            session,
            filter.toEncodeContext(stateContext),
            update.toEncodeContext(stateContext),
            options
        ).standardize()

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute() =
        getCollection<Template>().updateMany(
            filter.toEncodeContext(stateContext),
            update.toEncodeContext(stateContext),
            options
        ).standardize()

}

@Serializable
@SerialName("bulk")
class BulkWriteExecutor(
    override val database: String,
    override val collection: String,
    val operations: List<WriteOperation>
) : CollectionExecutor(), MultiVariableExecutor {

    @Transient
    override val variablesRequired =
        operations.maxOfOrNull { it.count } ?: 0

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute(): ExecutionResult =
        getCollection<EncodeContext>().bulkWrite(operations.flatMap {
            it.getWriteModels()
        }).standardize()

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute(session: ClientSession): ExecutionResult =
        getCollection<EncodeContext>().bulkWrite(session, operations.flatMap {
            it.getWriteModels()
        }).standardize()

}

@Serializable
@SerialName("transaction")
class TransactionExecutor(
    val executors: List<MongoSessionExecutor>,
    @Serializable(TransactionOptionsSerializer::class) val options: TransactionOptions,
    val maxRetryTimeout: Duration = 120.seconds,
    val maxRetryAttempts: Int = 100,
) : Executor {

    override val variablesRequired: Int
        get() = TODO("Not yet implemented")

    context(ExecutionContext, ResourceRegistry)
    override suspend fun execute(): ExecutionResult =
        resourceScope {
            val session = mongoSession(mongoClient)
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