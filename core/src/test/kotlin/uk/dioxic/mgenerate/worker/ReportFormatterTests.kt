package uk.dioxic.mgenerate.worker

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoCursor
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.InsertManyOptions
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.InsertManyResult
import com.mongodb.client.result.UpdateResult
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import org.bson.BsonObjectId
import org.bson.Document
import org.bson.RawBsonDocument
import org.bson.conversions.Bson
import uk.dioxic.mgenerate.Template
import uk.dioxic.mgenerate.extensions.tps
import uk.dioxic.mgenerate.test.IS_NOT_GH_ACTION
import uk.dioxic.mgenerate.worker.report.ReportFormat
import uk.dioxic.mgenerate.worker.report.ReportFormatter
import uk.dioxic.mgenerate.worker.report.format
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ReportFormatterTests : FunSpec({

//    isolationMode = IsolationMode.InstancePerTest

    test("Single execution").config(enabled = IS_NOT_GH_ACTION) {
        val client = mockk<MongoClient>()
        val database = mockk<MongoDatabase>()
        val helloCommand = Document("hello", 1)

        every { client.getDatabase(any()) } returns database
        every { database.runCommand(any()) } returns Document("ok", 1)

        val stage = SingleExecutionStage(
            name = "single stage",
            executor = CommandExecutor(
                client = client,
                command = helloCommand,
                database = "test"
            )
        )

        executeStages(stage, tick = 500.milliseconds)
            .format(ReportFormatter.create(ReportFormat.TEXT))
            .collect {
                print(it)
            }
    }

    test("Single workload").config(enabled = IS_NOT_GH_ACTION) {
        val client = mockk<MongoClient>()
        val collection = mockk<MongoCollection<Template>>()
        val result = InsertManyResult.acknowledged((0..5)
            .associateWith { BsonObjectId() })

        every {
            client
                .getDatabase(any())
                .getCollection(any(), Template::class.java)
        } returns collection

        every {
            collection.insertMany(any(), any<InsertManyOptions>())
        } returns result

        val stage = MultiExecutionStage(
            name = "stage",
            timeout = 5.seconds,
            workloads = listOf(
                Workload(
                    name = "really long workload name",
                    rate = 100.tps,
                    count = 100,
                    executor = InsertManyExecutor(
                        client = client,
                        db = "myDB",
                        collection = "myCollection",
                        template = Template(mapOf("name" to "Bob")),
                        number = 1
                    )
                )
            )
        )

        executeStage(stage)
            .format(ReportFormatter.create(ReportFormat.TEXT))
            .collect {
                print(it)
            }
    }


    test("Multiple workloads").config(enabled = IS_NOT_GH_ACTION) {
        val client = mockk<MongoClient>()
        val database = mockk<MongoDatabase>()
        val collection = mockk<MongoCollection<Template>>()
        val cursor = mockk<MongoCursor<RawBsonDocument>>()

        every {
            client.getDatabase(any())
        } returns database

        every {
            database.getCollection(any(), Template::class.java)
        } returns collection

        every {
            collection.insertMany(any(), any<InsertManyOptions>())
        } returns InsertManyResult.acknowledged((0..5)
            .associateWith { BsonObjectId() })

        every {
            collection.updateMany(any<Bson>(), any<Bson>(), any<UpdateOptions>())
        } returns UpdateResult.acknowledged(10, 5, BsonObjectId())

        every {
            collection.deleteMany(any<Bson>())
        } returns DeleteResult.acknowledged(100)

        every {
            database
                .getCollection("myCollection", RawBsonDocument::class.java)
                .find(any<Template>())
                .iterator()
        } returns cursor

        every { cursor.next() } returns RawBsonDocument(ByteArray(10))
        every { cursor.hasNext() } returnsMany List(100) { true } + false

        val stage = MultiExecutionStage(
            name = "stage",
            timeout = 5.seconds,
            workloads = listOf(
                Workload(
                    name = "really long insert workload",
                    rate = 100.tps,
                    count = 100,
                    executor = InsertManyExecutor(
                        client = client,
                        db = "myDB",
                        collection = "myCollection",
                        template = Template(mapOf("name" to "Bob")),
                        number = 1
                    )
                ),
                Workload(
                    name = "update workload",
                    rate = 100.tps,
                    count = 100,
                    executor = UpdateManyExecutor(
                        client = client,
                        db = "myDB",
                        collection = "myCollection",
                        filter = Template(mapOf("name" to "Bob")),
                        update = Template(mapOf("\$set" to mapOf("name" to "Alice")))
                    )
                ),
                Workload(
                    name = "delete workload",
                    rate = 100.tps,
                    count = 100,
                    executor = DeleteManyExecutor(
                        client = client,
                        db = "myDB",
                        collection = "myCollection",
                        filter = Template(mapOf("name" to "Bob")),
                    )
                ),
                Workload(
                    name = "find workload",
                    rate = 100.tps,
                    count = 100,
                    executor = FindExecutor(
                        client = client,
                        db = "myDB",
                        collection = "myCollection",
                        filter = Template(mapOf("name" to "Bob"))
                    )
                )
            )
        )

        executeStage(stage)
            .format(ReportFormatter.create(ReportFormat.TEXT))
            .collect {
                print(it)
            }
    }

})