package uk.dioxic.mgenerate.cli.commands

import arrow.fx.coroutines.resourceScope
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.mongodb.MongoClientSettings
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.put
import uk.dioxic.mgenerate.cli.checkConnection
import uk.dioxic.mgenerate.cli.options.*
import uk.dioxic.mgenerate.execute.buildBenchmark
import uk.dioxic.mgenerate.execute.execute
import uk.dioxic.mgenerate.execute.format.ReportFormat
import uk.dioxic.mgenerate.execute.format.ReportFormatter
import uk.dioxic.mgenerate.execute.format.format
import uk.dioxic.mgenerate.execute.model.CommandExecutor
import uk.dioxic.mgenerate.execute.model.InsertOneExecutor
import uk.dioxic.mgenerate.execute.model.TpsRate
import uk.dioxic.mgenerate.execute.model.UnlimitedRate
import uk.dioxic.mgenerate.execute.mongodb.cached
import uk.dioxic.mgenerate.execute.resources.ResourceRegistry
import uk.dioxic.mgenerate.execute.resources.mongoClient
import uk.dioxic.mgenerate.template.Template
import uk.dioxic.mgenerate.template.buildTemplate
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class Load : CliktCommand(help = "Load data directly into MongoDB") {
    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    private val authOptions by AuthOptions().cooccurring()
    private val connOptions by ConnectionOptions()
    private val namespaceOptions by NamespaceOptions()
    private val number by option("-n", "--number", help = "number of documents to load").long().default(100)
    private val tps by option("--tps", help = "target transactions per second").int()
        .check("TPS must be positive") { it > 0 }
    private val batchSize by option("-b", "--batchsize", help = "number of operations to batch together")
        .int()
        .default(100)
    private val workers by option(help = "number of workers").int().default(4)
    private val drop by option(help = "drop collection before load").flag()
    private val ordered by option(help = "enable ordered writes").flag()
    private val template by argument(name = "template").file(
        mustBeReadable = true,
        mustExist = true,
        canBeDir = false
    ).convert { Json.decodeFromString<Template>(it.readText()) }

    @OptIn(ExperimentalTime::class)
    override fun run() {

        val mcs = MongoClientSettings.builder()
            .applyAuthOptions(authOptions)
            .applyConnectionOptions(connOptions)
            .codecRegistry(Template.defaultRegistry)
            .build()

        val amendedBatchSize = min(batchSize, number.toInt())
        val amendedRate = tps?.div(amendedBatchSize)?.let { TpsRate(it) } ?: UnlimitedRate

        val benchmark = buildBenchmark {
            sequentialStage("main") {
                if (drop) {
                    rateWorkload(
                        name = "drop ${namespaceOptions.collection}",
                        count = 1,
                        executor = CommandExecutor(
                            database = namespaceOptions.database,
                            command = buildTemplate {
                                put("drop", namespaceOptions.collection)
                            }
                        )
                    )
                }
                rateWorkload(
                    name = if (amendedBatchSize == 1) "insertOne" else "insertMany",
                    count = number / amendedBatchSize,
                    rate = amendedRate,
                    executor = InsertOneExecutor(
                        database = namespaceOptions.database,
                        collection = namespaceOptions.collection,
                        template = template,
//                        number = amendedBatchSize,
//                        ordered = ordered,
                    )
                )
            }
        }

        println("Starting load...")


        val duration = runBlocking {
            resourceScope {
                val client = mongoClient(mcs)
                val registry = ResourceRegistry(client.cached())

                if (!checkConnection(client)) {
                    error("Can't connect!")
                }
                measureTime {
                    benchmark.execute(registry, workers)
                        .format(ReportFormatter.create(ReportFormat.TEXT))
                        .collect {
                            print(it)
                        }
                }
            }
        }

        println("\nCompleted in $duration (${(number / duration.toDouble(DurationUnit.SECONDS)).roundToInt()} inserts/s)")

    }

}
