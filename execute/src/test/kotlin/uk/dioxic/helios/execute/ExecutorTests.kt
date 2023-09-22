package uk.dioxic.helios.execute

import com.mongodb.MongoBulkWriteException
import com.mongodb.bulk.BulkWriteError
import com.mongodb.client.MongoClient
import com.mongodb.client.model.DeleteOptions
import com.mongodb.client.model.InsertManyOptions
import com.mongodb.client.model.InsertOneModel
import com.mongodb.client.model.WriteModel
import com.mongodb.client.result.InsertManyResult
import io.kotest.assertions.any
import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import org.bson.BsonDocument
import org.bson.BsonString
import org.bson.Document
import org.bson.RawBsonDocument
import org.bson.conversions.Bson
import uk.dioxic.helios.execute.model.*
import uk.dioxic.helios.execute.resources.buildResourceRegistry
import uk.dioxic.helios.execute.results.CommandResult
import uk.dioxic.helios.execute.results.ReadResult
import uk.dioxic.helios.execute.results.TimedExceptionResult
import uk.dioxic.helios.execute.results.TimedResult
import uk.dioxic.helios.execute.test.bulkWriteResult
import uk.dioxic.helios.execute.test.mockFindIterable
import uk.dioxic.helios.execute.test.mongoBulkWriteException
import uk.dioxic.helios.generate.*
import uk.dioxic.helios.generate.operators.NameOperator
import uk.dioxic.helios.generate.operators.VarOperator

class ExecutorTests : FunSpec({

    test("command") {
        val client = mockk<MongoClient> {
            every { getDatabase(any()) } returns mockk {
                every { runCommand(any()) } returns Document("ok", 1.0)
            }
        }
        val ctx = defaultExecutionContext.copy(
            executor = CommandExecutor(
                database = "test",
                command = buildTemplate {
                    put("drop", "myCollection")
                }
            )
        )
        val registry = buildResourceRegistry {
            mongoClient = client
        }

        val result = with(registry) {
            with(ctx) {
                executor.execute()
            }
        }

        result.shouldBeInstanceOf<CommandResult>().should {
            it.success shouldBe true
            it.document.shouldNotBeNull()
        }
    }

    test("find") {
        val client = mockk<MongoClient> {
            every { getDatabase(any()) } returns mockk {
                every { getCollection(any(), any<Class<RawBsonDocument>>()) } returns mockk {
                    every { find(any<Bson>()) } returns mockFindIterable(
                        RawBsonDocument.parse("{}"),
                        RawBsonDocument.parse("{}"),
                        RawBsonDocument.parse("{}")
                    )
                }
            }
        }

        val registry = buildResourceRegistry {
            mongoClient = client
        }
        val ctx = defaultExecutionContext.copy(
            executor = FindExecutor(
                database = "test",
                collection = "test",
                filter = Template.EMPTY
            )
        )

        val result = with(registry) {
            with(ctx) {
                executor.execute()
            }
        }

        result.shouldBeInstanceOf<ReadResult>().should {
            it.docsReturned shouldBe 3
        }
    }

    test("insertMany") {
        val variables = buildTemplate {
            putOperator<NameOperator>("vName")
        }
        val template = buildTemplate {
            putKeyedOperator<VarOperator>("name", "vName")
        }
        val documents = mutableListOf<List<EncodeContext>>()
        val client = mockk<MongoClient> {
            every { getDatabase(any()) } returns mockk {
                every { getCollection(any(), any<Class<EncodeContext>>()) } returns mockk {
                    every {
                        insertMany(capture(documents), any<InsertManyOptions>())
                    } returns InsertManyResult.acknowledged(mapOf(0 to BsonString("id")))
                }
            }
        }
        val executor = InsertManyExecutor(
            database = "test",
            collection = "test",
            template = template,
            size = 100
        )
        val ctx = defaultExecutionContext.copy {
            ExecutionContext.executor.set(executor)
            ExecutionContext.workload.rateWorkload.variables.set(variables)
            ExecutionContext.stateContext.set(List(executor.modelSize) {
                StateContext(it.toLong(), variables = variables.hydrateAndFlatten())
            })
        }
        val registry = buildResourceRegistry {
            mongoClient = client
        }

        with(registry) {
            with(ctx) {
                executor.execute()
            }
        }

        documents.shouldHaveSize(1)
        documents.forAll { encodeCtxList ->
            encodeCtxList.shouldHaveSize(100)
            encodeCtxList.map { it.operatorContext }.distinct().shouldHaveSize(100)
        }

    }

    context("bulk") {

        suspend fun executeBulk(
            client: MongoClient,
            operations: List<WriteOperation>,
            variables: Template = Template.EMPTY
        ): TimedResult {
            val executor = BulkWriteExecutor(
                database = "myDB",
                collection = "myCollection",
                operations = operations
            )

            val ctx = defaultExecutionContext.copy {
                ExecutionContext.executor.set(executor)
                ExecutionContext.workload.rateWorkload.variables.set(variables)
                ExecutionContext.stateContext.set(List(executor.modelSize) {
                    StateContext(it.toLong(), variables = variables.hydrateAndFlatten())
                })
            }
            val registry = buildResourceRegistry {
                mongoClient = client
            }

            return with(registry) {
                ctx.invoke()
            }
        }

        test("write models have correct different contexts") {
            val requests = mutableListOf<List<WriteModel<EncodeContext>>>()
            val client = mockk<MongoClient> {
                every { getDatabase(any()) } returns mockk {
                    every { getCollection(any(), any<Class<EncodeContext>>()) } returns mockk {
                        every { bulkWrite(capture(requests)) } returns bulkWriteResult(
                            insertCount = 1,
                            matchedCount = 1,
                            deletedCount = 1,
                            modifiedCount = 1
                        )
                    }
                }
            }

            val variables = buildTemplate {
                putOperator<NameOperator>("vName")
            }
            val template = buildTemplate {
                putKeyedOperator<VarOperator>("name", "vName")
            }
            val operations = listOf(
                InsertOneOperation(10, template),
                DeleteOneOperation(10, template, DeleteOptions()),
                DeleteManyOperation(10, template, DeleteOptions())
            )

            executeBulk(client, operations, variables)

            requests.first().should { writeModels ->
                writeModels.shouldHaveSize(30)
                val contexts = writeModels
                    .filterIsInstance<InsertOneModel<EncodeContext>>()
                    .map { it.document.operatorContext }

                contexts.onEach {
                    println(it.variables)
                }.distinct().count() shouldBe 10
            }

        }

        test("bulk write exception") {
            val client = mockk<MongoClient> {
                every { getDatabase(any()) } returns mockk {
                    every { getCollection(any(), any<Class<EncodeContext>>()) } returns mockk {
                        every { bulkWrite(any()) } throws mongoBulkWriteException(
                            bulkWriteResult(1, 2, 3, 4, 5),
                            listOf(BulkWriteError(11000, "duplicate key ex", BsonDocument(), 0))
                        )
                    }
                }
            }

            val template = buildTemplate {
                putKeyedOperator<VarOperator>("name", "vName")
            }
            val operations = listOf(
                InsertOneOperation(10, template),
                DeleteOneOperation(10, template, DeleteOptions()),
                DeleteManyOperation(10, template, DeleteOptions())
            )

            executeBulk(client, operations)
                .shouldBeInstanceOf<TimedExceptionResult>()
                .value
                .shouldBeInstanceOf<MongoBulkWriteException>()
                .should { errRes ->
                    errRes.writeResult.should {
                        it.insertedCount shouldBe 1
                        it.matchedCount shouldBe 2
                        it.modifiedCount shouldBe 3
                        it.deletedCount shouldBe 4
                        it.upserts shouldHaveSize 5
                    }
                    errRes.writeErrors shouldHaveSize 1
                }
        }
    }

})