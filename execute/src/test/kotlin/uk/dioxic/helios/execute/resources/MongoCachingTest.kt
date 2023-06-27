package uk.dioxic.helios.execute.resources

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bson.Document
import uk.dioxic.helios.execute.mongodb.CachedMongoDatabase
import uk.dioxic.helios.execute.mongodb.cached

class MongoCachingTest : FunSpec({

    test("mongo database is cached") {

        val client = mockk<MongoClient>()
        val database1 = mockk<MongoDatabase>()
        val database2 = mockk<MongoDatabase>()

        every { client.getDatabase("db1") } returns database1
        every { client.getDatabase("db2") } returns database2

        val resource = client.cached()

        repeat(5) {
            resource.getDatabase("db1").shouldBeInstanceOf<CachedMongoDatabase>()
        }
        repeat(5) {
            resource.getDatabase("db2").shouldBeInstanceOf<CachedMongoDatabase>()
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

        val resource = client.cached()

        repeat(5) {
            resource
                .getDatabase("db")
                .getCollection("collection1", Document::class.java) shouldBe collection1
        }
        repeat(5) {
            resource
                .getDatabase("db")
                .getCollection("collection2", Document::class.java) shouldBe collection2
        }

        verify(exactly = 1) { client.getDatabase(any()) }
        verify(exactly = 1) { database.getCollection("collection1", Document::class.java) }
        verify(exactly = 1) { database.getCollection("collection2", Document::class.java) }
    }

})