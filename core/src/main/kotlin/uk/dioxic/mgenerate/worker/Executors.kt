@file:Suppress("MemberVisibilityCanBePrivate")

package uk.dioxic.mgenerate.worker

import com.mongodb.MongoCommandException
import com.mongodb.client.MongoClient
import com.mongodb.client.model.Aggregates.project
import com.mongodb.client.model.InsertManyOptions
import com.mongodb.client.model.UpdateOptions
import org.bson.Document
import org.bson.RawBsonDocument
import org.bson.conversions.Bson
import uk.dioxic.mgenerate.Template
import uk.dioxic.mgenerate.worker.results.CommandResult
import uk.dioxic.mgenerate.worker.results.MessageResult
import uk.dioxic.mgenerate.worker.results.ReadResult
import uk.dioxic.mgenerate.worker.results.Result

sealed interface Executor<T : ExecutionContext> {
    fun invoke(workerId: Int, context: T): Result
}

sealed interface BaseExecutor : Executor<BaseExecutionContext> {
    fun createContext(): BaseExecutionContext
}

sealed interface MongoExecutor<T : ExecutionContext> : Executor<T> {
    fun createContext(client: MongoClient): T
}

sealed interface DatabaseExecutor : MongoExecutor<DatabaseExecutionContext> {
    val database: String
    override fun createContext(client: MongoClient) =
        DatabaseExecutionContext(client, this)
}

sealed interface CollectionExecutor<TDocument> : MongoExecutor<CollectionExecutionContext<TDocument>> {
    val database: String
    val collection: String
    val documentClass: Class<TDocument>
    override fun createContext(client: MongoClient) =
        CollectionExecutionContext(client, this)
}

open class CommandExecutor(
    override val database: String = "admin",
    val command: Bson,
) : DatabaseExecutor {

    override fun invoke(workerId: Int, context: DatabaseExecutionContext): Result = CommandResult(
        try {
            context.mongoDatabase.runCommand(command)["ok"] == 1
        } catch (e: MongoCommandException) {
            false
        }
    )
}

class DropExecutor(
    database: String,
    val collection: String,
) : CommandExecutor(database, Document("drop", collection))

class MessageExecutor(
    val messageFn: (Int) -> String,
) : BaseExecutor {
    override fun createContext() = BaseExecutionContext(this)

    override fun invoke(workerId: Int, context: BaseExecutionContext): Result =
        MessageResult(messageFn(workerId))
}

class InsertOneExecutor(
    override val database: String,
    override val collection: String,
    val template: Template,
) : CollectionExecutor<Template> {
    override val documentClass = Template::class.java

    override fun invoke(workerId: Int, context: CollectionExecutionContext<Template>): Result =
        context.mongoCollection.insertOne(template).standardize()
}

class InsertManyExecutor(
    override val database: String,
    override val collection: String,
    val ordered: Boolean = true,
    val number: Int,
    val template: Template,
) : CollectionExecutor<Template> {
    override val documentClass = Template::class.java

    private val documents = List(number) { template }
    private val options = InsertManyOptions().ordered(ordered)

    override fun invoke(workerId: Int, context: CollectionExecutionContext<Template>): Result =
        context.mongoCollection.insertMany(documents, options).standardize()
}

class UpdateOneExecutor(
    override val database: String,
    override val collection: String,
    val filter: Template,
    val update: Template,
    val updateOptions: UpdateOptions = UpdateOptions()
) : CollectionExecutor<Template> {
    override val documentClass = Template::class.java

    override fun invoke(workerId: Int, context: CollectionExecutionContext<Template>): Result =
        context.mongoCollection.updateOne(filter, update, updateOptions).standardize()
}

class UpdateManyExecutor(
    override val database: String,
    override val collection: String,
    val filter: Template,
    val update: Template,
    val updateOptions: UpdateOptions = UpdateOptions()
) : CollectionExecutor<Template> {
    override val documentClass = Template::class.java

    override fun invoke(workerId: Int, context: CollectionExecutionContext<Template>): Result =
        context.mongoCollection.updateMany(filter, update, updateOptions).standardize()
}

class DeleteOneExecutor(
    override val database: String,
    override val collection: String,
    val filter: Template,
) : CollectionExecutor<Template> {
    override val documentClass = Template::class.java

    override fun invoke(workerId: Int, context: CollectionExecutionContext<Template>): Result =
        context.mongoCollection.deleteOne(filter).standardize()
}

class DeleteManyExecutor(
    override val database: String,
    override val collection: String,
    val filter: Template,
) : CollectionExecutor<Template> {
    override val documentClass = Template::class.java

    override fun invoke(workerId: Int, context: CollectionExecutionContext<Template>): Result =
        context.mongoCollection.deleteMany(filter).standardize()
}

class FindExecutor(
    override val database: String,
    override val collection: String,
    val skip: Int? = null,
    val limit: Int? = null,
    val sort: Document? = null,
    val project: Document? = null,
    val filter: Template,
) : CollectionExecutor<RawBsonDocument> {
    override val documentClass = RawBsonDocument::class.java

    override fun invoke(workerId: Int, context: CollectionExecutionContext<RawBsonDocument>): Result =
        ReadResult(context.mongoCollection.find(filter).apply {
            if (project != null) project(project)
            if (limit != null) limit(limit)
            if (sort != null) sort(sort)
            if (skip != null) skip(skip)
        }.count())
}