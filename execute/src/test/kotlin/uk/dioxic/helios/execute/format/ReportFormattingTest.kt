package uk.dioxic.helios.execute.format

import com.mongodb.DuplicateKeyException
import com.mongodb.MongoException
import com.mongodb.ServerAddress
import com.mongodb.WriteConcernResult
import com.mongodb.client.MongoClient
import com.mongodb.client.result.InsertOneResult
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.buffer
import org.bson.BsonDocument
import org.bson.BsonObjectId
import org.bson.BsonString
import uk.dioxic.helios.execute.buildBenchmark
import uk.dioxic.helios.execute.defaultExecutor
import uk.dioxic.helios.execute.defaultMongoExecutor
import uk.dioxic.helios.execute.execute
import uk.dioxic.helios.execute.model.ExecutionContext
import uk.dioxic.helios.execute.model.MessageExecutor
import uk.dioxic.helios.execute.model.TpsRate
import uk.dioxic.helios.execute.resources.ResourceRegistry
import uk.dioxic.helios.execute.resources.buildResourceRegistry
import uk.dioxic.helios.execute.test.IS_NOT_GH_ACTION
import uk.dioxic.helios.generate.Template
import kotlin.time.Duration

class ReportFormattingTest : FunSpec({
    val executor = mockk<MessageExecutor>()
    val errorExecutor = mockk<MessageExecutor> {
        coEvery {
            with(any<ResourceRegistry>()) {
                with(any<ExecutionContext>()) {
                    execute()
                }
            }
        } throws MongoException("myError") andThenThrows DuplicateKeyException(
            BsonDocument("response", BsonString("error")),
            ServerAddress("example", 27017),
            WriteConcernResult.acknowledged(1, true, null)
        )
    }
    val client = mockk<MongoClient> {
        every { getDatabase(any()).getCollection(any(), any<Class<Template>>()) } returns mockk {
            every { insertOne(any()) } returns InsertOneResult.acknowledged(BsonObjectId())
        }
    }
    val benchmark = buildBenchmark {
        parallelStage {
            addRateWorkload(executor = defaultExecutor, count = 100, rate = TpsRate(15))
            addRateWorkload(executor = defaultExecutor, count = 100, rate = TpsRate(50))
            addRateWorkload(executor = errorExecutor, count = 100, rate = TpsRate(30))
            addRateWorkload(executor = defaultMongoExecutor, count = 100, rate = TpsRate(60))
        }
        sequentialStage {
            rateWorkload(executor = defaultExecutor)
            rateWorkload(executor = errorExecutor, count = 50, rate = TpsRate(80))
            rateWorkload(executor = defaultExecutor, count = 100, rate = TpsRate(100))
            rateWorkload(executor = defaultExecutor, count = 100, rate = TpsRate(50))
        }
    }
    val registry = runBlocking {
        buildResourceRegistry {
            mongoClient = client
        }
    }

    afterTest {
        clearMocks(executor)
    }

    xcontext("text format") {
        test("print multiple workloads").config(enabled = IS_NOT_GH_ACTION) {
            benchmark.execute(registry)
                .buffer(100)
                .format(ReportFormatter.create(ReportFormat.TEXT)).collect {
                    println(it)
                }
        }
        test("print unsummarized workloads").config(enabled = IS_NOT_GH_ACTION) {
            buildBenchmark {
                parallelStage {
                    addRateWorkload(executor = defaultExecutor, count = 20, rate = TpsRate(2))
                    addRateWorkload(executor = defaultMongoExecutor, count = 5, rate = TpsRate(3))
                }
            }.execute(registry = registry, interval = Duration.ZERO)
                .buffer(100)
                .format(ReportFormatter.create(ReportFormat.TEXT)).collect {
                    println(it)
                }
        }
    }

    xcontext("json format") {
        test("print multiple workloads").config(enabled = IS_NOT_GH_ACTION) {
            benchmark.execute(registry)
                .buffer(100)
                .format(ReportFormatter.create(ReportFormat.JSON)).collect {
                    println(it)
                }
        }
    }

})