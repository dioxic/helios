@file:Suppress("MemberVisibilityCanBePrivate")

package uk.dioxic.mgenerate.worker

import com.mongodb.client.MongoClient
import com.mongodb.client.model.DeleteOptions
import com.mongodb.client.model.UpdateOptions
import org.bson.conversions.Bson
import uk.dioxic.mgenerate.Template
import uk.dioxic.mgenerate.worker.results.CommandResult
import uk.dioxic.mgenerate.worker.results.MessageResult
import uk.dioxic.mgenerate.worker.results.ReadResult
import uk.dioxic.mgenerate.worker.results.Result

fun interface Executor {
    fun invoke(workerId: Int): Result
}

class CommandExecutor(
    client: MongoClient,
    val command: Bson,
    database: String = "admin",
) : Executor {
    private val database = client.getDatabase(database)

    override fun invoke(workerId: Int) = CommandResult(
        database.runCommand(command)
    )

}

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
        .getCollection(collection)

    override fun invoke(workerId: Int) =
        mongoCollection.insertOne(template).standardize()
}

class InsertManyExecutor(
    client: MongoClient,
    db: String,
    collection: String,
    val number: Int = 1,
    val template: Template,
) : Executor {

    private val mongoCollection = client.getDatabase(db)
        .getCollection(collection)

    override fun invoke(workerId: Int) =
        mongoCollection.insertMany(List(number) {template}).standardize()
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
        .getCollection(collection)

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
        .getCollection(collection)

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
        .getCollection(collection)

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
        .getCollection(collection)

    override fun invoke(workerId: Int) =
        mongoCollection.deleteMany(filter).standardize()
}

class FindExecutor(
    client: MongoClient,
    db: String,
    collection: String,
    val filter: Template,
) : Executor {

    private val mongoCollection = client.getDatabase(db)
        .getCollection(collection)

    override fun invoke(workerId: Int) =
        ReadResult(mongoCollection.find(filter).count())
}