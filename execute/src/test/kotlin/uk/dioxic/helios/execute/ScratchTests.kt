package uk.dioxic.helios.execute

import arrow.fx.coroutines.parMap
import arrow.fx.coroutines.resourceScope
import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKeys
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.bson.Bson
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import uk.dioxic.helios.execute.model.Benchmark
import uk.dioxic.helios.execute.model.BulkWriteExecutor
import uk.dioxic.helios.execute.model.InsertOneOperation
import uk.dioxic.helios.execute.model.MessageExecutor
import uk.dioxic.helios.execute.resources.ResourceRegistry
import uk.dioxic.helios.execute.resources.fileSink
import uk.dioxic.helios.execute.test.mapMessageResults
import uk.dioxic.helios.generate.buildTemplate
import uk.dioxic.helios.generate.operators.ChooseOperator
import uk.dioxic.helios.generate.operators.IntOperator
import uk.dioxic.helios.generate.operators.NameOperator
import uk.dioxic.helios.generate.operators.VarOperator
import uk.dioxic.helios.generate.putKeyedOperator
import uk.dioxic.helios.generate.putOperator
import uk.dioxic.helios.generate.putOperatorObject
import uk.dioxic.helios.generate.serialization.TemplateExecutionSerializer
import kotlin.time.Duration
import kotlin.time.measureTime


@OptIn(DelicateCoroutinesApi::class, FlowPreview::class, ExperimentalCoroutinesApi::class)
class ScratchTests : FunSpec({

    test("mockk exension") {
        mockkStatic("kotlin.collections.CollectionsKt")

        val l: Iterable<Int> = listOf(1, 2)

        every { any<Iterable<Int>>().count() } returns 3

//        l.count() shouldBe 3
    }

    test("random file read") {
        val fakeFs = FakeFileSystem()
        val root = "/".toPath()
        val dictionaryFile = root / "dictionary.json"

        fakeFs.createDirectories(root)
        val dictionary = """
            {"i": 1}
            {"i": 2}
            {"i": 3}
            {"i": { "nested": 123 }}
        """.trimIndent()
        fakeFs.write(dictionaryFile) {
            writeUtf8(dictionary)
        }

        dictionaryFile
            .asFlow(fakeFs)
            .take(6)
            .collect {
                println(it)
            }
    }

    test("write to file") {
        val template = buildTemplate {
            putOperator<IntOperator>("name")
        }

        resourceScope {
            val f = "c:\\users\\Mark\\desktop\\dictionary.json"
            val sink = fileSink(FileSystem.SYSTEM, f)
            val stream = sink.outputStream()

//            val writer = JsonWriter(FileWriter(f))
//            val codec = DocumentCodec()

            measureTime {
                flow {
                    repeat(1_000_000) {
//                        emit("{\"name\": \"Ms. Lance Stark\"}")
                        emit(template)
                    }
                }.parMap(20) {
////                    Bson.encodeToString(TemplateExecutionSerializer, it)
                    Bson.encodeToByteArray(TemplateExecutionSerializer, it)
                }.flowOn(Dispatchers.Default)
                    .collect {
                        sink.write(it)
//                    codec.encode(writer, it, EncoderContext.builder().build())
//                    Bson.encodeToByteArray(TemplateExecutionSerializer, it)

//                        Bson.encodeToStream(TemplateExecutionSerializer, it, sink.outputStream())
//                    sink.writeUtf8(it)
//                    sink.writeUtf8("\n")
                    }
//                writer.flush()
//                writer.close()
            }
        }.also {
            println("Completed in $it")
        }

    }

    test("quick") {
        (0 until 1).forEach {
            println(it)
        }
    }

    test("double flow") {

        val shared = flow {
            var count = 0
            while (true) {
                emit(count++)
            }
        }.buffer(10)
            .shareIn(
                scope = GlobalScope,
                started = SharingStarted.StartWhenSubscribedAtLeast(2),
                replay = 0
            )

        val limit = flowOf(0, 1, 2, 3, 4, 5)
        val longLimit = (0..1_000_000).asFlow()

        launch {
            shared.zip(limit) { f, l ->
                "C1 $l $f"
            }.collect {
                println(it)
            }
        }
        launch {
            shared.zip(longLimit) { f, l ->
                l to f
            }.collect {
                if (it.first % 100_000 == 0) {
                    println("C2 ${it.first} ${it.second}")
                }
            }
            println("done C2")
        }

        launch {
            delay(1500)
            println("starting **")
            shared.zip(limit) { f, l ->
                "** $l $f"
            }.collect {
                println(it)
            }
//            shared.take(4).collect {
//                println(it)
//            }
        }

//        val shared = flow.shareIn(GlobalScope, SharingStarted.WhileSubscribedAtLeast(2))
//
//        launch {
//            shared.
//        }

    }

    test("sharedflow") {

        val variables = flow {
            var count = 0
            while (true) {
                emit(count++)
            }
        }.shareIn(
            scope = GlobalScope,
//            started = SharingStarted.Eagerly,
            started = SharingStarted.StartWhenSubscribedAtLeast(2),
//            started = SharingStarted.WhileSubscribed(),
            replay = 50
        )

//        variables.collect {
//            println(it)
//        }

        val ex = (0..100).asFlow()

        delay(100)

        launch(Dispatchers.Default) {
            variables.zip(ex) { v, e ->
                "Z1 $e-$v"
            }.onEach {
                println(it)
            }.collect()
        }

        launch(Dispatchers.Default) {
            ex.zip(variables) { e, v ->
                "Z2 $e-$v"
            }.onEach {
                println(it)
                delay(10)
            }.collect()
        }


    }

    test("bulk executor") {

        val stageVariables = buildTemplate {
            putOperatorObject<ChooseOperator>("cities") {
                putBsonArray("from") {
                    addAll(listOf("Belfast", "London", "Madrid"))
                }
            }
            putOperator<NameOperator>("name")
        }
        val workloadVariables1 = buildTemplate {
            put("species", "bird")
        }
        val template = buildTemplate {
            putKeyedOperator<VarOperator>("name", "name")
            putKeyedOperator<VarOperator>("species", "species")
            putKeyedOperator<VarOperator>("cities", "cities")
        }

        val stage = buildParallelStage {
            variables = stageVariables
            sync = true
            addRateWorkload(
                variables = workloadVariables1,
                executor = BulkWriteExecutor(
                    database = "myDatabase",
                    collection = "myCollection",
                    operations = listOf(
                        InsertOneOperation(100, template),
                        InsertOneOperation(120, template)
                    )
                ),
                count = 10
            )
        }

        with(ResourceRegistry.EMPTY) {
            produceExecutions(Benchmark.EMPTY, stage).toList().should { exCtxList ->
                exCtxList shouldHaveSize 10
                exCtxList.count { it.executor is BulkWriteExecutor } shouldBe 10
                exCtxList.forAll { ctx ->
                    ctx.stateContext shouldHaveSize 120
                    ctx.stateContext.distinctBy { it.variables } shouldHaveSize 120
                }
            }
        }
    }

    test("variables are sync'd across workloads") {
        val stageVariables = buildTemplate {
            putOperatorObject<ChooseOperator>("cities") {
                putBsonArray("from") {
                    addAll(listOf("Belfast", "London", "Madrid"))
                }
            }
            putOperator<NameOperator>("name")
        }
        val workloadVariables1 = buildTemplate {
            put("species", "bird")
        }
        val workloadVariables2 = buildTemplate {
            put("species", "mammal")
        }
        val template = buildTemplate {
            putKeyedOperator<VarOperator>("name", "name")
            putKeyedOperator<VarOperator>("species", "species")
            putKeyedOperator<VarOperator>("cities", "cities")
        }
        val benchmark = buildBenchmark {
            parallelStage {
                variables = stageVariables
                sync = true
                addRateWorkload(
                    variables = workloadVariables1,
                    executor = MessageExecutor(template),
                    count = 10
                )
                addRateWorkload(
                    variables = workloadVariables2,
                    executor = MessageExecutor(template),
                    count = 10
                )
            }
        }
        benchmark.execute(interval = Duration.ZERO)
            .mapMessageResults()
            .onEach { it.doc.shouldContainKeys("name", "cities", "species") }
            .map { it.doc.filter { (k, _) -> k != "species" } }
            .toList()
            .distinct().should { d ->
                d.count() shouldBe 10
            }
    }

})