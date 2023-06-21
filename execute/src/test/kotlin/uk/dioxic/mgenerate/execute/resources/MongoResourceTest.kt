package uk.dioxic.mgenerate.execute.resources

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bson.Document

class MongoResourceTest : FunSpec({

    test("mongo database is cached") {

        val client = mockk<MongoClient>()
        val database1 = mockk<MongoDatabase>()
        val database2 = mockk<MongoDatabase>()

        every { client.getDatabase("db1") } returns database1
        every { client.getDatabase("db2") } returns database2

        val resource = MongoResource(client)

        repeat(5) {
            resource.getDatabase("db1") shouldBe database1
        }
        repeat(5) {
            resource.getDatabase("db2") shouldBe database2
        }

        verify(exactly = 1) { client.getDatabase("db1") }
        verify(exactly = 1) { client.getDatabase("db2") }
    }

    test("mongo collection is cached") {

        val client = mockk<MongoClient>()
        val database = mockk<MongoDatabase>()
        val collection1 = mockk<MongoCollection<Document>>()
        val collection2 = mockk<MongoCollection<Document>>()

        every { client.getDatabase(any()) } returns database

        every {
            database.getCollection("collection1", Document::class.java)
        } returns collection1

        every {
            database.getCollection("collection2", Document::class.java)
        } returns collection2

        val resource = MongoResource(client)

        repeat(5) {
            resource.getCollection<Document>("db", "collection1") shouldBe collection1
        }
        repeat(5) {
            resource.getCollection<Document>("db", "collection2") shouldBe collection2
        }

        verify(exactly = 2) { client.getDatabase(any()) }
        verify(exactly = 1) { database.getCollection("collection1", Document::class.java) }
        verify(exactly = 1) { database.getCollection("collection2", Document::class.java) }
    }

})