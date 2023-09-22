package uk.dioxic.helios.execute

import uk.dioxic.helios.execute.model.*
import uk.dioxic.helios.generate.Template
import uk.dioxic.helios.generate.buildTemplate
import java.time.LocalDateTime
import java.util.*

val defaultTemplate = buildTemplate {
    put("name", "\$name")
    put("date", LocalDateTime.now())
    put("uuid", UUID.randomUUID())
    put("long", Long.MAX_VALUE)
}

val defaultExecutor = MessageExecutor(Template.EMPTY)

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
    name = "stage0",
    workloads = listOf(defaultWorkload)
)

val defaultSequentialStage = SequentialStage(
    name = "stage0",
    workloads = listOf(defaultWorkload)
)

val defaultStage = defaultSequentialStage

val defaultBenchmark = Benchmark(
    name = "benchmark0",
    stages = listOf(defaultStage)
)

val defaultStateContext = StateContext()

val defaultExecutionContext = ExecutionContext(
    workload = defaultWorkload,
    rate = defaultWorkload.rate,
    stateContext = listOf(defaultStateContext),
    count = 0L
)