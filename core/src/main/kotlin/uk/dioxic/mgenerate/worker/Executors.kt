@file:Suppress("MemberVisibilityCanBePrivate")

package uk.dioxic.mgenerate.worker

import com.mongodb.client.MongoClient
import org.bson.conversions.Bson
import uk.dioxic.mgenerate.Template
import uk.dioxic.mgenerate.worker.results.CommandResult
import uk.dioxic.mgenerate.worker.results.MessageResult
import uk.dioxic.mgenerate.worker.results.Result

fun interface Executor {
    fun invoke(workerId: Int): Result
}

class CommandExecutor(
    client: MongoClient,
    val command: Bson
) : Executor {
    private val database = client.getDatabase("admin")

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