package uk.dioxic.mgenerate.worker.model

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import uk.dioxic.mgenerate.extensions.measureTimedResult
import uk.dioxic.mgenerate.worker.results.TimedResult

sealed interface ExecutionContext {
    val workload: Workload
    val executor: Executor<*>
    val state: ExecutionState
    operator fun invoke(): TimedResult
    fun withState(state: ExecutionState): ExecutionContext
}

data class CollectionExecutionContext<TDocument>(
    override val executor: Executor<CollectionExecutionContext<TDocument>>,
    override val workload: Workload,
    override val state: ExecutionState,
    val mongoCollection: MongoCollection<TDocument>,
) : ExecutionContext {
    override fun invoke(): TimedResult = measureTimedResult(workload.name) {
        executor.execute(this)
    }

    override fun withState(state: ExecutionState) = this.copy(state = state)
}

data class DatabaseExecutionContext(
    override val executor: Executor<DatabaseExecutionContext>,
    override val workload: Workload,
    override val state: ExecutionState,
    val mongoDatabase: MongoDatabase,
) : ExecutionContext {
    override fun invoke(): TimedResult = measureTimedResult(workload.name) {
        executor.execute(this)
    }

    override fun withState(state: ExecutionState) = this.copy(state = state)
}

fun Workload.createContext(client: MongoClient, stageState: State): ExecutionContext {
    val executionState = ExecutionState(
        custom = (stageState + hydratedState).custom,
        executionCount = 0,
        startTimeMillis = System.currentTimeMillis()
    )
    return when (executor) {
        is CollectionExecutor<*> -> (executor as CollectionExecutor<*>).createContext(client, this, executionState)
        is DatabaseExecutor -> (executor as DatabaseExecutor).createContext(client, this, executionState)
    }
}

private fun <TDocument> CollectionExecutor<TDocument>.createContext(
    client: MongoClient,
    workload: Workload,
    state: ExecutionState
) =
    CollectionExecutionContext(
        executor = this,
        workload = workload,
        state = state,
        mongoCollection = client.getDatabase(database).getCollection(collection, documentClass)
    )

private fun DatabaseExecutor.createContext(
    client: MongoClient,
    workload: Workload,
    state: ExecutionState
) =
    DatabaseExecutionContext(
        executor = this,
        workload = workload,
        state = state,
        mongoDatabase = client.getDatabase(database)
    )