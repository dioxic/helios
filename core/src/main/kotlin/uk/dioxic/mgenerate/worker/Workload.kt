package uk.dioxic.mgenerate.worker

import com.mongodb.client.MongoClient
import uk.dioxic.mgenerate.extensions.measureTimedResult

sealed interface Workload : Named {
    override val name: String
    val executor: Executor<*>
    val weight: Int
    val rate: Rate
    val executionCount: Long
}

data class BaseWorkload(
    override val name: String,
    override val executor: BaseExecutor,
    override val weight: Int = 1,
    override val rate: Rate = Rate.MAX,
    override val executionCount: Long = Long.MAX_VALUE,
) : Workload {
    operator fun invoke(workerId: Int, context: BaseExecutionContext) = measureTimedResult {
        executor.invoke(workerId, context)
    }

    fun createContext() =
        WorkloadContext(this, executor.createContext())
}

data class MongoWorkload<T : ExecutionContext>(
    override val name: String,
    override val executor: MongoExecutor<T>,
    override val weight: Int = 1,
    override val rate: Rate = Rate.MAX,
    override val executionCount: Long = Long.MAX_VALUE,
) : Workload {
    operator fun invoke(workerId: Int, context: T) = measureTimedResult {
        executor.invoke(workerId, context)
    }

    fun createContext(client: MongoClient) =
        WorkloadContext(this, executor.createContext(client))
}

