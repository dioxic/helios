package uk.dioxic.mgenerate.worker

import com.mongodb.client.MongoClient
import uk.dioxic.mgenerate.Template
import uk.dioxic.mgenerate.worker.results.MessageResult
import uk.dioxic.mgenerate.worker.results.TimedWorkloadResult
import uk.dioxic.mgenerate.worker.results.measureTimedWorkloadValue

sealed interface Workload {
    val name: String
    fun execute(workerId: Int): TimedWorkloadResult
}

sealed interface MultiExecutionWorkload : Workload {
    override val name: String
    val weight: Int
    val rate: Rate
    val count: Long
    override fun execute(workerId: Int): TimedWorkloadResult
}

sealed interface SingleExecutionWorkload : Workload

class CommandWorkload(
    override val name: String
) : SingleExecutionWorkload {

    override fun execute(workerId: Int) = measureTimedWorkloadValue {
        MessageResult("hello")
    }
}

class MessageWorkload(
    override val name: String,
    override val weight: Int = 1,
    override val rate: Rate = Rate.MAX,
    override val count: Long,
    val messageFn: (Int) -> String,
) : MultiExecutionWorkload {

    override fun execute(workerId: Int) = measureTimedWorkloadValue {
        MessageResult(messageFn(workerId))
    }
}

class InsertOneWorkload(
    override val name: String,
    override val weight: Int = 1,
    override val rate: Rate = Rate.MAX,
    override val count: Long = Long.MAX_VALUE,
    client: MongoClient,
    db: String,
    collection: String,
    val template: Template,
) : MultiExecutionWorkload {

    private val mongoCollection = client.getDatabase(db)
        .getCollection(collection)

    override fun execute(workerId: Int) = measureTimedWorkloadValue {
        mongoCollection.insertOne(template).standardize()
    }
}