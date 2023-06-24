package uk.dioxic.mgenerate.execute.format

import com.mongodb.client.MongoClient
import com.mongodb.client.result.InsertOneResult
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.buffer
import org.bson.BsonObjectId
import uk.dioxic.mgenerate.execute.buildBenchmark
import uk.dioxic.mgenerate.execute.defaultExecutor
import uk.dioxic.mgenerate.execute.defaultMongoExecutor
import uk.dioxic.mgenerate.execute.execute
import uk.dioxic.mgenerate.execute.model.MessageExecutor
import uk.dioxic.mgenerate.execute.model.TpsRate
import uk.dioxic.mgenerate.execute.resources.ResourceRegistry
import uk.dioxic.mgenerate.execute.test.IS_NOT_GH_ACTION
import uk.dioxic.mgenerate.template.Template

class ReportFormattingTest : FunSpec({

    val executor = mockk<MessageExecutor>()
    val mongoClient = mockk<MongoClient> {
        every { getDatabase(any()).getCollection(any(), any<Class<Template>>()) } returns mockk {
            every { insertOne(any()) } returns InsertOneResult.acknowledged(BsonObjectId())
        }
    }
    val benchmark = buildBenchmark {
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
    }

    afterTest {
        clearMocks(executor)
    }

    context("text format") {
        test("print multiple workloads").config(enabled = IS_NOT_GH_ACTION) {
            benchmark.execute(ResourceRegistry(mongoClient))
                .buffer(100)
                .format(ReportFormatter.create(ReportFormat.TEXT)).collect {
                    println(it)
                }
        }
    }

    context("json format") {
        test("print multiple workloads").config(enabled = IS_NOT_GH_ACTION) {
            benchmark.execute(ResourceRegistry(mongoClient))
                .buffer(100)
                .format(ReportFormatter.create(ReportFormat.JSON)).collect {
                    println(it)
                }
        }
    }

})