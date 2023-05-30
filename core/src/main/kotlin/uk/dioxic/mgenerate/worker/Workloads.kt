package uk.dioxic.mgenerate.worker

import com.mongodb.client.MongoClient
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import uk.dioxic.mgenerate.Template
import uk.dioxic.mgenerate.worker.results.MessageResult
import uk.dioxic.mgenerate.worker.results.TimedWorkloadResult
import uk.dioxic.mgenerate.worker.results.measureTimedWorkloadValue
import kotlin.time.Duration.Companion.milliseconds

sealed interface Workload {
    val name: String
    val weight: Int
    val rate: Rate
    val count: Long?
    fun execute(workerId: Int): TimedWorkloadResult
}

class MessageWorkload(
    override val name: String,
    override val weight: Int = 1,
    override val rate: Rate = Rate.MAX,
    override val count: Long = 10000,
    val messageFn: (Int) -> String,
) : Workload {

    override fun execute(workerId: Int) = measureTimedWorkloadValue {
        MessageResult(messageFn(workerId))
    }
}

class InsertOneWorkload(
    override val name: String,
    override val weight: Int = 1,
    override val rate: Rate = Rate.MAX,
    override val count: Long?,
    client: MongoClient,
    db: String,
    collection: String,
    val template: Template,
) : Workload {

    private val mongoCollection = client.getDatabase(db)
        .getCollection(collection)

    override fun execute(workerId: Int) = measureTimedWorkloadValue {
        mongoCollection.insertOne(template).standardize()
    }
}