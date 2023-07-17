package uk.dioxic.helios.execute

import arrow.optics.copy
import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.MongoClient
import com.mongodb.client.model.DeleteOptions
import com.mongodb.client.model.InsertOneModel
import com.mongodb.client.model.WriteModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import org.bson.Document
import uk.dioxic.helios.execute.model.*
import uk.dioxic.helios.execute.resources.ResourceRegistry
import uk.dioxic.helios.execute.results.CommandResult
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

        val result = with(ResourceRegistry(client)) {
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

    context("bulk") {

        suspend fun getWriteModels(variables: Template, operations: List<WriteOperation>)
                : List<WriteModel<EncodeContext>> {
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

            with(ResourceRegistry(client)) {
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