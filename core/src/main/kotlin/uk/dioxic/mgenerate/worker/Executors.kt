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

sealed interface Executor {
    fun invoke(workerId: Int): Result
}

open class CommandExecutor(
    client: MongoClient,
    database: String = "admin",
    val command: Bson,
) : Executor {
    private val database = client.getDatabase(database)

    override fun invoke(workerId: Int) = CommandResult(
        try {
            database.runCommand(command)["ok"] == 1
        } catch (e: MongoCommandException) {
            false
        }
    )
}

class DropExecutor(
    client: MongoClient,
    db: String,
    collection: String,
) : CommandExecutor(client, db, Document("drop", collection))

class MessageExecutor(
    val messageFn: (Int) -> String,
) : Executor {
    override fun invoke(workerId: Int) = MessageResult(messageFn(workerId))
}

class InsertOneExecutor(
    client: MongoClient,
    db: String,
    collection: String,
    val template: Template,
) : Executor {

    private val mongoCollection = client.getDatabase(db)
        .getCollection(collection, Template::class.java)

    override fun invoke(workerId: Int) =
        mongoCollection.insertOne(template).standardize()
}

class InsertManyExecutor(
    client: MongoClient,
    db: String,
    collection: String,
    ordered: Boolean = true,
    val number: Int,
    val template: Template,
) : Executor {

    private val mongoCollection = client.getDatabase(db)
        .getCollection(collection, Template::class.java)

    private val documents = List(number) { template }
    private val options = InsertManyOptions().ordered(ordered)

    override fun invoke(workerId: Int) =
        mongoCollection.insertMany(documents, options).standardize()
}

class UpdateOneExecutor(
    client: MongoClient,
    db: String,
    collection: String,
    val filter: Template,
    val update: Template,
    val updateOptions: UpdateOptions = UpdateOptions()
) : Executor {

    private val mongoCollection = client.getDatabase(db)
        .getCollection(collection, Template::class.java)

    override fun invoke(workerId: Int) =
        mongoCollection.updateOne(filter, update, updateOptions).standardize()
}

class UpdateManyExecutor(
    client: MongoClient,
    db: String,
    collection: String,
    val filter: Template,
    val update: Template,
    val updateOptions: UpdateOptions = UpdateOptions()
) : Executor {

    private val mongoCollection = client.getDatabase(db)
        .getCollection(collection, Template::class.java)

    override fun invoke(workerId: Int) =
        mongoCollection.updateMany(filter, update, updateOptions).standardize()
}

class DeleteOneExecutor(
    client: MongoClient,
    db: String,
    collection: String,
    val filter: Template,
) : Executor {

    private val mongoCollection = client.getDatabase(db)
        .getCollection(collection, Template::class.java)

    override fun invoke(workerId: Int) =
        mongoCollection.deleteOne(filter).standardize()
}

class DeleteManyExecutor(
    client: MongoClient,
    db: String,
    collection: String,
    val filter: Template,
) : Executor {

    private val mongoCollection = client.getDatabase(db)
        .getCollection(collection, Template::class.java)

    override fun invoke(workerId: Int) =
        mongoCollection.deleteMany(filter).standardize()
}

class FindExecutor(
    client: MongoClient,
    db: String,
    collection: String,
    val skip: Int? = null,
    val limit: Int? = null,
    val sort: Document? = null,
    val project: Document? = null,
    val filter: Template,
) : Executor {

    private val mongoCollection = client.getDatabase(db)
        .getCollection(collection, RawBsonDocument::class.java)

    override fun invoke(workerId: Int) =
        ReadResult(mongoCollection.find(filter).apply {
            if (project != null) project(project)
            if (limit != null) limit(limit)
            if (sort != null) sort(sort)
            if (skip != null) skip(skip)
        }.count())
}