package uk.dioxic.helios.cli.commands

import arrow.fx.coroutines.resourceScope
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.mongodb.MongoClientSettings
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.bson.Bson
import kotlinx.serialization.decodeFromString
import uk.dioxic.helios.cli.checkConnection
import uk.dioxic.helios.cli.options.*
import uk.dioxic.helios.execute.buildBenchmark
import uk.dioxic.helios.execute.execute
import uk.dioxic.helios.execute.format.ReportFormat
import uk.dioxic.helios.execute.format.ReportFormatter
import uk.dioxic.helios.execute.format.format
import uk.dioxic.helios.execute.model.CommandExecutor
import uk.dioxic.helios.execute.model.InsertManyExecutor
import uk.dioxic.helios.execute.model.TpsRate
import uk.dioxic.helios.execute.model.UnlimitedRate
import uk.dioxic.helios.execute.resources.buildResourceRegistry
import uk.dioxic.helios.generate.Template
import uk.dioxic.helios.generate.buildTemplate
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.DurationUnit
import kotlin.time.measureTime
import uk.dioxic.helios.generate.codecs.DocumentCodec as HeliosDocumentCodec

class Load : CliktCommand(help = "Load data directly into MongoDB") {
    init {
        context { helpFormatter = { ctx -> MordantHelpFormatter(ctx, showDefaultValues = true)} }
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
    private val concurrency by option(help = "number of concurrent operations").int().default(4)
    private val outputFormat by option("-f", "--format", help = "output format")
        .enum<ReportFormat>().default(ReportFormat.TEXT)
    private val drop by option(help = "drop collection before load").flag()
    private val ordered by option(help = "enable ordered writes").flag()
    private val template by argument(name = "template").file(
        mustBeReadable = true,
        mustExist = true,
        canBeDir = false
    ).convert { Bson.decodeFromString<Template>(it.readText()) }

    override fun run() {

        val mcs = MongoClientSettings.builder()
            .applyAuthOptions(authOptions)
            .applyConnectionOptions(connOptions)
            .codecRegistry(HeliosDocumentCodec.defaultRegistry)
            .build()

        val amendedBatchSize = min(batchSize, number.toInt())
        val amendedRate = tps?.div(amendedBatchSize)?.let { TpsRate(it) } ?: UnlimitedRate

        val benchmark = buildBenchmark {
            sequentialStage {
                name = "main"
                if (drop) {
                    addRateWorkload(
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
                addRateWorkload(
                    name = if (amendedBatchSize == 1) "insertOne" else "insertMany",
                    count = number / amendedBatchSize,
                    rate = amendedRate,
                    executor = InsertManyExecutor(
                        database = namespaceOptions.database,
                        collection = namespaceOptions.collection,
                        template = template,
                        size = amendedBatchSize,
                        ordered = ordered,
                    )
                )
            }
        }

        runBlocking {
            resourceScope {
                val registry = buildResourceRegistry {
                    createMongoClient(mcs)
                }

                if (checkConnection(registry.mongoClient)) {
                    println("Starting load...")
                    val duration = measureTime {
                        benchmark.execute(registry, concurrency)
                            .format(ReportFormatter.create(outputFormat))
                            .collect {
                                println(it)
                            }
                    }
                    println("\nCompleted in $duration (${(number / duration.toDouble(DurationUnit.SECONDS)).roundToInt()} inserts/s)")
                }
            }
        }
    }
}
