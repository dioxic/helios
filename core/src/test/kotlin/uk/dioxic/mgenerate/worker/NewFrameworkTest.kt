package uk.dioxic.mgenerate.worker

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.result.InsertOneResult
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import org.bson.BsonObjectId
import uk.dioxic.mgenerate.Template
import uk.dioxic.mgenerate.resources.MongoResource
import uk.dioxic.mgenerate.resources.ResourceRegistry
import uk.dioxic.mgenerate.worker.model.*
import kotlin.time.Duration.Companion.seconds

class NewFrameworkTest : FunSpec({

    test("newExec") {
        val client = mockk<MongoClient>()
        val collection = mockk<MongoCollection<Template>>()

        val benchmark = buildBenchmark {
            sequentialStage {
                rateWorkload(
                    rate = PeriodRate(1.seconds, 0.5),
                    count = 5
                )
                rateWorkload(
                    count = 5
                )
            }
        }

        every {
            client.getDatabase(any()).getCollection(any(), any<Class<Template>>())
        } returns collection

        every { collection.insertOne(any()) } returns InsertOneResult.acknowledged(BsonObjectId())

        val registry = ResourceRegistry(MongoResource(client))

        executeBenchmark(benchmark, registry).collect {
            println(when(it) {
                is StageStartMessage -> "stage start"
                is StageCompleteMessage -> "stage complete"
                is WorkloadCompleteMessage -> TODO()
                is WorkloadProgressMessage -> it.message
                is WorkloadStartMessage -> TODO()
            })
        }

    }

})