package uk.dioxic.mgenerate.worker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import uk.dioxic.mgenerate.Template
import uk.dioxic.mgenerate.resources.MongoResource
import uk.dioxic.mgenerate.resources.ResourceRegistry
import uk.dioxic.mgenerate.worker.results.CommandResult
import uk.dioxic.mgenerate.worker.results.MessageResult
import uk.dioxic.mgenerate.worker.results.Result
import uk.dioxic.mgenerate.worker.standardize

@Serializable
sealed interface Executor {
    fun execute(context: ExecutionContext, resourceRegistry: ResourceRegistry): Result
}

@Serializable
sealed class CollectionExecutor : Executor {
    abstract val database: String
    abstract val collection: String

    inline fun <reified TDocument> getCollection(resourceRegistry: ResourceRegistry) =
        resourceRegistry[MongoResource::class].getCollection<TDocument>(database, collection)

}

@Serializable
sealed class DatabaseExecutor : Executor {
    abstract val database: String

    fun getDatabase(resourceRegistry: ResourceRegistry) =
        resourceRegistry[MongoResource::class].getDatabase(database)
}

@Serializable
data class MessageExecutor(
    val message: String
): Executor {
    override fun execute(context: ExecutionContext, resourceRegistry: ResourceRegistry) =
        MessageResult(message)

}

@Serializable
@SerialName("command")
data class CommandExecutor(
    override val database: String,
    val command: Template
) : DatabaseExecutor() {
    override fun execute(context: ExecutionContext, resourceRegistry: ResourceRegistry) = CommandResult(
        getDatabase(resourceRegistry).runCommand(command)
    )
}

@Serializable
@SerialName("insertOne")
data class InsertOneExecutor(
    override val database: String,
    override val collection: String,
    val template: Template
) : CollectionExecutor() {
    override fun execute(context: ExecutionContext, resourceRegistry: ResourceRegistry) =
        getCollection<Template>(resourceRegistry).insertOne(template).standardize()

}