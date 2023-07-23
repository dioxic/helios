package uk.dioxic.helios.execute

import arrow.optics.copy
import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.MongoClient
import com.mongodb.client.model.DeleteOptions
import com.mongodb.client.model.InsertManyOptions
import com.mongodb.client.model.InsertOneModel
import com.mongodb.client.model.WriteModel
import com.mongodb.client.result.InsertManyResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import org.bson.BsonString
import org.bson.Document
import org.bson.RawBsonDocument
import org.bson.conversions.Bson
import uk.dioxic.helios.execute.model.*
import uk.dioxic.helios.execute.resources.buildResourceRegistry
import uk.dioxic.helios.execute.results.CommandResult
import uk.dioxic.helios.execute.results.ReadResult
import uk.dioxic.helios.execute.test.mockFindIterable
import uk.dioxic.helios.generate.*
import uk.dioxic.helios.generate.operators.NameOperator
import uk.dioxic.helios.generate.operators.VarOperator

class ExecutorTests : FunSpec({

    test("command executor") {
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
            it.failureCount shouldBe 0
            it.successCount shouldBe 1
            it.document.shouldNotBeNull()
        }
    }

    test("find executor") {
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

    test("insertMany executor") {
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
            ExecutionContext.workload.rateWorkload.variablesDefinition.set(variables)
            ExecutionContext.stateContext.set(List(executor.variablesRequired) {
                StateContext(it.toLong(), variables = lazy { variables.hydrateAndFlatten() })
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

        suspend fun getWriteModels(
            variables: Template,
            operations: List<WriteOperation>
        ): List<WriteModel<EncodeContext>> {
            val requests = mutableListOf<List<WriteModel<EncodeContext>>>()
            val client = mockk<MongoClient> {
                every { getDatabase(any()) } returns mockk {
                    every { getCollection(any(), any<Class<EncodeContext>>()) } returns mockk {
                        every { bulkWrite(capture(requests)) } returns BulkWriteResult.acknowledged(
                            1, 1, 1, 4, emptyList(), emptyList()
                        )
                    }
                }
            }

            val executor = BulkWriteExecutor(
                database = "myDB",
                collection = "myCollection",
                operations = operations
            )

            val ctx = defaultExecutionContext.copy {
                ExecutionContext.executor.set(executor)
                ExecutionContext.workload.rateWorkload.variablesDefinition.set(variables)
                ExecutionContext.stateContext.set(List(executor.variablesRequired) {
                    StateContext(it.toLong(), variables = lazy { variables.hydrateAndFlatten() })
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

            return requests.first()
        }

        test("write models have correct different contexts") {
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

            getWriteModels(variables, operations).should { writeModels ->
                writeModels.shouldHaveSize(30)
                val contexts = writeModels
                    .filterIsInstance<InsertOneModel<EncodeContext>>()
                    .map { it.document.operatorContext }

                contexts.onEach {
                    println(it.variables.value)
                }.distinct().count() shouldBe 10
            }

        }
    }

})