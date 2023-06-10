package uk.dioxic.mgenerate.worker

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoCursor
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.InsertManyOptions
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.InsertManyResult
import com.mongodb.client.result.InsertOneResult
import com.mongodb.client.result.UpdateResult
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.of
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bson.BsonObjectId
import org.bson.Document
import org.bson.RawBsonDocument
import uk.dioxic.mgenerate.Template
import uk.dioxic.mgenerate.test.sew
import uk.dioxic.mgenerate.worker.results.TimedCommandResult
import uk.dioxic.mgenerate.worker.results.TimedMessageResult
import uk.dioxic.mgenerate.worker.results.TimedReadResult
import uk.dioxic.mgenerate.worker.results.TimedWriteResult

class ExecutorTests : FunSpec({
    isolationMode = IsolationMode.InstancePerTest

    val updateResultArb = arbitrary {
        val matched = Arb.long(0L..50).bind()
        val modified = Arb.long(0L..50).bind()
        val upsertedId = Arb.of(null, BsonObjectId()).bind()
        UpdateResult.acknowledged(matched, modified, upsertedId)
    }

    val client = mockk<MongoClient>()
    val database = mockk<MongoDatabase>()
    val collection = mockk<MongoCollection<Template>>()

    every { client.getDatabase(any()) } returns database
    every { database.getCollection(any(), Template::class.java) } returns collection

    test("message executor") {
        val workload = sew(MessageExecutor { "hello worker $it" })
        val result = workload.invoke(0)

        result.shouldBeInstanceOf<TimedMessageResult>()
        result.workloadName shouldBe "myWorkload"
        result.value.msg shouldStartWith "hello worker"
        result.value.msg shouldEndWith "0"
    }

    listOf(
        "success" to 1,
        "failure" to 0,
    ).forEach {
        test("command executor ${it.first}") {
            val helloCommand = Document("hello", 1)
            val workload = sew(CommandExecutor(client, helloCommand, "test"))

            every { database.runCommand(any()) } returns Document("ok", it.second)

            workload.invoke(0).apply {
                shouldBeInstanceOf<TimedCommandResult>()
                workloadName shouldBe workload.name
                value.success shouldBe (it.second == 1)
            }

            verify { client.getDatabase("test") }
            verify { database.runCommand(any()) }
        }
    }

    test("insertOne executor") {
        val workload = sew(
            InsertOneExecutor(
                client = client,
                db = "myDB",
                collection = "myCollection",
                template = Template(mapOf("name" to "Bob"))
            )
        )

        every { collection.insertOne(any()) } returns InsertOneResult.acknowledged(BsonObjectId())

        workload.invoke(0).apply {
            shouldBeInstanceOf<TimedWriteResult>()
            workloadName shouldBe workload.name
            value.insertCount shouldBe 1
            value.deletedCount shouldBe 0
            value.matchedCount shouldBe 0
            value.modifiedCount shouldBe 0
            value.upsertedCount shouldBe 0
        }

        verify { client.getDatabase("myDB") }
        verify { database.getCollection("myCollection", Template::class.java) }
        verify { collection.insertOne(any()) }
    }

    test("insertMany executor") {
        checkAll(Arb.int(0..5)) { docCount ->
            val result = (0..docCount).associateWith { BsonObjectId() }

            every {
                collection.insertMany(any(), any<InsertManyOptions>())
            } returns InsertManyResult.acknowledged(result)

            val workload = sew(
                InsertManyExecutor(
                    client = client,
                    db = "myDB",
                    collection = "myCollection",
                    template = Template(mapOf("name" to "Bob")),
                    number = 1
                )
            )

            workload.invoke(0).apply {
                shouldBeInstanceOf<TimedWriteResult>()
                workloadName shouldBe workload.name
                value.insertCount shouldBe result.count()
                value.deletedCount shouldBe 0
                value.matchedCount shouldBe 0
                value.modifiedCount shouldBe 0
                value.upsertedCount shouldBe 0
            }

            verify { client.getDatabase("myDB") }
            verify { database.getCollection("myCollection", Template::class.java) }
            verify { collection.insertMany(any(), any<InsertManyOptions>()) }
        }
    }

    test("updateOne executor") {
        checkAll(updateResultArb) { updateResult ->
            val filter = Template(mapOf("name" to "Bob"))
            val update = Template(mapOf("\$set" to mapOf("name" to "Alice")))
            val workload = sew(
                UpdateOneExecutor(
                    client = client,
                    db = "myDB",
                    collection = "myCollection",
                    filter = filter,
                    update = update,
                )
            )

            every { collection.updateOne(filter, update, any()) } returns updateResult

            workload.invoke(0).apply {
                shouldBeInstanceOf<TimedWriteResult>()
                workloadName shouldBe workload.name
                value.insertCount shouldBe 0
                value.deletedCount shouldBe 0
                value.matchedCount shouldBe updateResult.matchedCount
                value.modifiedCount shouldBe updateResult.modifiedCount
                value.upsertedCount shouldBe if (updateResult.upsertedId != null) 1 else 0
            }

            verify { client.getDatabase("myDB") }
            verify { database.getCollection("myCollection", Template::class.java) }
            verify { collection.updateOne(filter, update, any()) }
        }
    }

    test("updateMany executor") {
        checkAll(updateResultArb) { updateResult ->
            val filter = Template(mapOf("name" to "Bob"))
            val update = Template(mapOf("\$set" to mapOf("name" to "Alice")))
            val workload = sew(
                UpdateManyExecutor(
                    client = client,
                    db = "myDB",
                    collection = "myCollection",
                    filter = filter,
                    update = update,
                )
            )

            every { collection.updateMany(filter, update, any()) } returns updateResult

            workload.invoke(0).apply {
                shouldBeInstanceOf<TimedWriteResult>()
                workloadName shouldBe workload.name
                value.insertCount shouldBe 0
                value.deletedCount shouldBe 0
                value.matchedCount shouldBe updateResult.matchedCount
                value.modifiedCount shouldBe updateResult.modifiedCount
                value.upsertedCount shouldBe if (updateResult.upsertedId != null) 1 else 0
            }

            verify { client.getDatabase("myDB") }
            verify { database.getCollection("myCollection", Template::class.java) }
            verify { collection.updateMany(filter, update, any()) }
        }
    }

    test("deleteOne executor") {
        checkAll(Arb.int(0..5)) { docCount ->
            every {
                collection.deleteOne(any())
            } returns DeleteResult.acknowledged(docCount.toLong())

            val workload = sew(
                DeleteOneExecutor(
                    client = client,
                    db = "myDB",
                    collection = "myCollection",
                    filter = Template(mapOf("name" to "Bob"))
                )
            )

            workload.invoke(0).apply {
                shouldBeInstanceOf<TimedWriteResult>()
                workloadName shouldBe workload.name
                value.insertCount shouldBe 0
                value.deletedCount shouldBe docCount
                value.matchedCount shouldBe 0
                value.modifiedCount shouldBe 0
                value.upsertedCount shouldBe 0
            }

            verify { client.getDatabase("myDB") }
            verify { database.getCollection("myCollection", Template::class.java) }
            verify { collection.deleteOne(any()) }
        }
    }

    test("deleteMany executor") {
        checkAll(Arb.int(0..5)) { docCount ->
            every {
                collection.deleteMany(any())
            } returns DeleteResult.acknowledged(docCount.toLong())

            val workload = sew(
                DeleteManyExecutor(
                    client = client,
                    db = "myDB",
                    collection = "myCollection",
                    filter = Template(mapOf("name" to "Bob"))
                )
            )

            workload.invoke(0).apply {
                shouldBeInstanceOf<TimedWriteResult>()
                workloadName shouldBe workload.name
                value.insertCount shouldBe 0
                value.deletedCount shouldBe docCount
                value.matchedCount shouldBe 0
                value.modifiedCount shouldBe 0
                value.upsertedCount shouldBe 0
            }

            verify { client.getDatabase("myDB") }
            verify { database.getCollection("myCollection", Template::class.java) }
            verify { collection.deleteMany(any()) }
        }
    }

    test("find executor") {
        checkAll(Arb.int(0..5)) { docCount ->
            val cursor = mockk<MongoCursor<RawBsonDocument>>()

            every {
                database
                    .getCollection("myCollection", RawBsonDocument::class.java)
                    .find(any<Template>())
                    .iterator()
            } returns cursor

            every { cursor.next() } returns RawBsonDocument(ByteArray(10))
            every { cursor.hasNext() } returnsMany List(docCount) { true } + false

            val workload = sew(
                FindExecutor(
                    client = client,
                    db = "myDB",
                    collection = "myCollection",
                    filter = Template(mapOf("name" to "Bob"))
                )
            )

            workload.invoke(0).apply {
                shouldBeInstanceOf<TimedReadResult>()
                workloadName shouldBe workload.name
                value.docReturned shouldBe docCount
            }

            verify { client.getDatabase("myDB") }
            verify { database.getCollection("myCollection", RawBsonDocument::class.java) }
//            verify { collection.find(any<Template>()) }
        }
    }

})