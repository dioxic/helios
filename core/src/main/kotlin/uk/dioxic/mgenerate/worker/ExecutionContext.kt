package uk.dioxic.mgenerate.worker

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import uk.dioxic.mgenerate.worker.results.Result

sealed interface ExecutionContext {
    fun invoke(workerId: Int): Result
}

class BaseExecutionContext(
    private val executor: Executor<BaseExecutionContext>
) : ExecutionContext {
    override fun invoke(workerId: Int): Result =
        executor.invoke(workerId, this)
}

class CollectionExecutionContext<TDocument>(
    client: MongoClient,
    private val executor: CollectionExecutor<TDocument>
) : ExecutionContext {
    val mongoCollection: MongoCollection<TDocument> = client
        .getDatabase(executor.database)
        .getCollection(executor.collection, executor.documentClass)

    override fun invoke(workerId: Int): Result =
        executor.invoke(workerId, this)
}

class DatabaseExecutionContext(
    client: MongoClient,
    private val executor: DatabaseExecutor
) : ExecutionContext {
    val mongoDatabase: MongoDatabase = client.getDatabase(executor.database)

    override fun invoke(workerId: Int): Result =
        executor.invoke(workerId, this)
}