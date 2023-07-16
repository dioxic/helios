package uk.dioxic.helios.execute

import com.mongodb.client.MongoClient
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import org.bson.Document
import uk.dioxic.helios.execute.model.CommandExecutor
import uk.dioxic.helios.execute.model.Executor
import uk.dioxic.helios.execute.resources.ResourceRegistry
import uk.dioxic.helios.execute.results.CommandResult
import uk.dioxic.helios.generate.buildTemplate

class ExecutorTests : FunSpec({

    fun executeBenchmark(executor: Executor, client: MongoClient) =
        buildBenchmark {
            sequentialStage {
                rateWorkload(executor = executor)
            }
        }.execute(ResourceRegistry(client))

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

})