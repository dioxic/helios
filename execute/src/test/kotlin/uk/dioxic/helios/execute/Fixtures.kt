package uk.dioxic.helios.execute

import kotlinx.serialization.json.put
import uk.dioxic.helios.execute.model.*
import uk.dioxic.helios.generate.buildTemplate
import uk.dioxic.helios.generate.put
import java.time.LocalDateTime
import java.util.*

val defaultTemplate = buildTemplate {
    put("name", "\$name")
    put("date", LocalDateTime.now())
    put("uuid", UUID.randomUUID())
    put("long", Long.MAX_VALUE)
}

val defaultExecutor = MessageExecutor("hello world!")

val defaultMongoExecutor = InsertOneExecutor(
    database = "myDB",
    collection = "myCollection",
    template = defaultTemplate
)

val defaultWorkload = RateWorkload(
    name = "workload0",
    executor = defaultExecutor
)

val defaultParallelStage = ParallelStage(
    name= "stage0",
    workloads = listOf(defaultWorkload)
)

val defaultSequentialStage = SequentialStage(
    name= "stage0",
    workloads = listOf(defaultWorkload)
)

val defaultStage = defaultSequentialStage

val defaultBenchmark = Benchmark(
    name = "benchmark0",
    stages = listOf(defaultStage)
)

val defaultExecutionContext = defaultWorkload.createContext(defaultBenchmark, defaultStage)
