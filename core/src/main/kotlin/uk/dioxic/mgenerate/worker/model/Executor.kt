package uk.dioxic.mgenerate.worker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import uk.dioxic.mgenerate.Template
import uk.dioxic.mgenerate.worker.results.CommandResult
import uk.dioxic.mgenerate.worker.results.Result
import uk.dioxic.mgenerate.worker.standardize

@Serializable
sealed interface Executor<T: ExecutionContext> {
    fun execute(context: T): Result
}

sealed interface CollectionExecutor<TDocument> : Executor<CollectionExecutionContext<TDocument>> {
    val database: String
    val collection: String
    val documentClass: Class<TDocument>
}

sealed interface DatabaseExecutor : Executor<DatabaseExecutionContext> {
    val database: String
}

@Serializable
@SerialName("command")
data class CommandExecutor(
    override val database: String,
    val command: Template
) : DatabaseExecutor {
    override fun execute(context: DatabaseExecutionContext) = CommandResult(
        context.mongoDatabase.runCommand(command)
    )
}

@Serializable
@SerialName("insertOne")
data class InsertOneExecutor(
    override val database: String,
    override val collection: String,
    val template: Template
) : CollectionExecutor<Template> {
    override fun execute(context: CollectionExecutionContext<Template>) =
        context.mongoCollection.insertOne(template).standardize()

    override val documentClass: Class<Template>
        get() = Template::class.java
}