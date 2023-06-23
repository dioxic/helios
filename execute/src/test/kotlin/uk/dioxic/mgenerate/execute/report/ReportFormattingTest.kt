package uk.dioxic.mgenerate.execute.report

import com.mongodb.client.result.InsertOneResult
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.buffer
import org.bson.BsonObjectId
import org.bson.Document
import org.bson.conversions.Bson
import uk.dioxic.mgenerate.execute.buildBenchmark
import uk.dioxic.mgenerate.execute.defaultExecutor
import uk.dioxic.mgenerate.execute.defaultMongoExecutor
import uk.dioxic.mgenerate.execute.execute
import uk.dioxic.mgenerate.execute.model.MessageExecutor
import uk.dioxic.mgenerate.execute.model.TpsRate
import uk.dioxic.mgenerate.execute.resources.MongoResource
import uk.dioxic.mgenerate.execute.resources.ResourceRegistry
import uk.dioxic.mgenerate.template.Template
import kotlin.time.Duration.Companion.seconds

class ReportFormattingTest : FunSpec({

    val executor = mockk<MessageExecutor>()
    val mongoResource = mockk<MongoResource> {
        every { getDatabase(any()) } returns mockk {
            every { runCommand(any<Bson>()) } returns Document("ok", 1)
        }
        every { getCollection<Template>(any(), any()) } returns mockk {
            every { insertOne(any()) } returns InsertOneResult.acknowledged(BsonObjectId())
        }
    }

    afterTest {
        clearMocks(executor)
    }

    test("print multiple workloads") {
//        runBlocking {
        buildBenchmark {
            parallelStage {
                rateWorkload(executor = defaultExecutor, count = 100, rate = TpsRate(15))
                rateWorkload(executor = defaultExecutor, count = 100, rate = TpsRate(50))
                rateWorkload(executor = defaultMongoExecutor, count = 100, rate = TpsRate(60))
            }
            sequentialStage {
                rateWorkload(executor = defaultExecutor)
                rateWorkload(executor = defaultExecutor, count = 100, rate = TpsRate(100))
                rateWorkload(executor = defaultExecutor, count = 100, rate = TpsRate(50))
            }
        }.execute(ResourceRegistry(mongoResource))
            .buffer(100)
            .format(ReportFormatter.create(ReportFormat.TEXT)).collect {
                print(it)
            }
//        }
    }

    test("scratch") {

        val wpr = WorkloadProgressReport(
            workloadName = "workload1",
            insertedCount = 100,
            progress = 50,
            operationCount = 200,
            elapsed = 4.seconds
        ).toMap()

        println(wpr)

    }


})