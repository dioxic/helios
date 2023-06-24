package uk.dioxic.mgenerate.execute.model

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import uk.dioxic.mgenerate.execute.resources.ResourceRegistry
import uk.dioxic.mgenerate.execute.results.CommandResult
import uk.dioxic.mgenerate.execute.results.ExecutionResult
import uk.dioxic.mgenerate.execute.results.MessageResult
import uk.dioxic.mgenerate.execute.results.standardize
import uk.dioxic.mgenerate.template.Template

@Serializable
sealed interface Executor {
    context(ExecutionContext, ResourceRegistry)
    fun execute(): ExecutionResult
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

    context(ExecutionContext)
    override fun execute() =
        MessageResult("$message - count: $executionCount")

}

@Serializable
@SerialName("command")
data class CommandExecutor(
    override val database: String,
    val command: Template
) : DatabaseExecutor() {

    context(ExecutionContext, ResourceRegistry)
    override fun execute() = CommandResult(
        getDatabase().runCommand(command)
    )
}

@Serializable
@SerialName("insertOne")
data class InsertOneExecutor(
    override val database: String,
    override val collection: String,
    val template: Template
) : CollectionExecutor() {

    context(ExecutionContext, ResourceRegistry)
    override fun execute() =
        getCollection<Template>().insertOne(template).standardize()

}