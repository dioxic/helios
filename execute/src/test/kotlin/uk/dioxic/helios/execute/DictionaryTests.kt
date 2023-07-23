package uk.dioxic.helios.execute

import arrow.fx.coroutines.resourceScope
import com.mongodb.MongoNamespace
import com.mongodb.client.MongoClient
import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKeys
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.bson.Bson
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.bson.Document
import uk.dioxic.helios.execute.model.*
import uk.dioxic.helios.execute.resources.ResourceRegistry
import uk.dioxic.helios.execute.resources.buildResourceRegistry
import uk.dioxic.helios.execute.test.mockAggregateIterable
import uk.dioxic.helios.execute.test.mockFindIterable
import uk.dioxic.helios.generate.buildTemplate
import uk.dioxic.helios.generate.operators.ArrayOperator
import uk.dioxic.helios.generate.operators.ChooseOperator
import uk.dioxic.helios.generate.operators.NameOperator
import uk.dioxic.helios.generate.operators.ObjectIdOperator
import uk.dioxic.helios.generate.putOperator
import uk.dioxic.helios.generate.putOperatorObject
import uk.dioxic.helios.generate.serialization.DocumentSerializer

class DictionaryTests : FunSpec({

    context("Serialization") {
        val dc = mapOf(
            "query" to QueryDictionary(
                namespace = MongoNamespace("test.example"),
                filter = buildTemplate {
                    putBsonDocument("age") {
                        put("\$gt", 10)
                    }
                },
                select = listOf("age", "name")
            ),
            "sample" to SampleDictionary(
                namespace = MongoNamespace("test.example"),
                size = 100,
                select = listOf("name")
            ),
            "stream" to StreamDictionary(
                template = buildTemplate {
                    putOperator<NameOperator>("name")
                }
            ),
            "constant" to ConstantDictionary(
                template = buildTemplate {
                    putOperatorObject<ArrayOperator>("ids") {
                        putOperator<ObjectIdOperator>("of")
                        put("number", 1000)
                    }
                }
            )
        )

        val json = """{
            | "query": { "type": "query", "ns": "test.example", "filter": { "age": { "${'$'}gt": 10} }, "select": ["age", "name"] },
            | "sample": { "type": "sample", "ns": "test.example", "select": ["name"], "size": 100 },
            | "stream": { "type": "stream", "template": { "name": "${'$'}name" } },
            | "constant": { "type": "constant", "template": { "ids": { "${'$'}array": { "of": "${'$'}objectId", "number": 1000 } } } } }
        """.trimMargin()

        test("serialize") {
            Bson.encodeToString(dc) shouldBeJson json
        }

        test("deserialize") {
            val map = Bson.decodeFromString<Map<String, Dictionary>>(json)
            Bson.encodeToString(map) shouldBeJson json
        }
    }

    context("Flows") {
        test("query") {
            val client = mockk<MongoClient> {
                every { getDatabase(any()) } returns mockk {
                    every { getCollection(any()) } returns mockk {
                        every { find(any<org.bson.conversions.Bson>()) } answers {
                            mockFindIterable(
                                Document("name", "Badger"),
                                Document("name", "Giraffe"),
                                Document("name", "Turtle"),
                            )
                        }
                    }
                }
            }
            val dictionary = QueryDictionary(
                namespace = MongoNamespace("test", "animal"),
            )
            resourceScope {
                val resourceRegistry = buildResourceRegistry {
                    mongoClient = client
                }

                with(resourceRegistry) {
                    dictionary.asFlow().take(10).toList().should { values ->
                        values.shouldHaveSize(10)
                        values.forAll {
                            it.shouldContainKeys("name")
                        }
                    }
                }
            }
        }
        test("sample") {
            val client = mockk<MongoClient> {
                every { getDatabase(any()) } returns mockk {
                    every { getCollection(any()) } returns mockk {
                        every { aggregate(any()) } answers {
                            mockAggregateIterable(
                                Document("name", "Badger"),
                                Document("name", "Giraffe"),
                                Document("name", "Turtle"),
                            )
                        }
                    }
                }
            }
            val dictionary = SampleDictionary(
                namespace = MongoNamespace("test", "animal"),
                size = 3
            )
            resourceScope {
                val resourceRegistry = buildResourceRegistry {
                    mongoClient = client
                }

                with(resourceRegistry) {
                    dictionary.asFlow().take(10).toList().should { values ->
                        values.shouldHaveSize(10)
                        values.forAll {
                            it.shouldContainKeys("name")
                        }
                    }
                }
            }
        }
        test("stream") {
            val dictionary = StreamDictionary(
                template = buildTemplate {
                    putOperatorObject<ChooseOperator>("number") {
                        putBsonArray("from") {
                            addAll(listOf(1, 2, 3))
                        }
                    }
                }
            )
            with(ResourceRegistry.EMPTY) {
                dictionary.asFlow().take(10).toList().should { values ->
                    values.shouldHaveSize(10)
                    values.forAll {
                        it.shouldContainKeys("number")
                    }
                }
            }
        }
        test("constant") {
            val dictionary = ConstantDictionary(
                template = buildTemplate {
                    putOperator<NameOperator>("name")
                }
            )
            with(ResourceRegistry.EMPTY) {
                dictionary.asFlow().take(10).toList().should { values ->
                    values.shouldHaveSize(10)
                    values.distinct().count() shouldBe 1
                    values.forAll {
                        it.shouldContainKeys("name")
                    }
                }
            }
        }
    }

    context("Stores") {
        fun fakeFile(filename: String, contents: List<Document>): FakeFileSystem {
            val fakeFs = FakeFileSystem()
            val root = "/".toPath()
            val dictionaryFile = root / filename

            fakeFs.createDirectories(root)
            fakeFs.write(dictionaryFile) {
                contents.forEach {
                    writeUtf8(Bson.encodeToString(DocumentSerializer, it))
                    writeUtf8("\n")
                }
            }
            return fakeFs
        }

        test("Store.YES and file exists") {
            val fakeFs = fakeFile(
                filename = "animal.$defaultStoreExtension",
                contents = listOf(
                    Document("species", "Badger"),
                    Document("species", "Giraffe"),
                    Document("species", "Turtle"),
                )
            )
            val dictionary = QueryDictionary(
                namespace = MongoNamespace("test", "animal"),
                store = Store.YES
            )
            with(ResourceRegistry.EMPTY) {
                dictionary.asResourcedFlow("animal", fakeFs).take(10)
                    .onEach {
                        println(it)
                    }.toList().should { values ->
                        values.shouldHaveSize(10)
                        values.forAll {
                            it.shouldContainKeys("species")
                        }
                    }
            }
        }
        test("PathStore and file exists") {
            val fakeFs = fakeFile(
                filename = "custom.example",
                contents = listOf(
                    Document("species", "Badger"),
                    Document("species", "Giraffe"),
                    Document("species", "Turtle"),
                )
            )
            val dictionary = QueryDictionary(
                namespace = MongoNamespace("test", "animal"),
                store = PathStore("custom.example")
            )
            with(ResourceRegistry.EMPTY) {
                dictionary.asResourcedFlow("animal", fakeFs).take(10)
                    .onEach {
                        println(it)
                    }.toList().should { values ->
                        values.shouldHaveSize(10)
                        values.forAll {
                            it.shouldContainKeys("species")
                        }
                    }
            }
        }
    }
})