package uk.dioxic.mgenerate.cli.commands

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
import com.mongodb.client.MongoClients
import kotlinx.coroutines.runBlocking
import uk.dioxic.mgenerate.Template
import uk.dioxic.mgenerate.cli.checkConnection
import uk.dioxic.mgenerate.cli.options.*
import uk.dioxic.mgenerate.worker.*
import uk.dioxic.mgenerate.worker.report.ReportFormat
import uk.dioxic.mgenerate.worker.report.ReportFormatter
import uk.dioxic.mgenerate.worker.report.format
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
    ).convert { Template.parse(it.readText()) }

    @OptIn(ExperimentalTime::class)
    override fun run() {

        val client = MongoClients.create(
            MongoClientSettings.builder()
                .applyAuthOptions(authOptions)
                .applyConnectionOptions(connOptions)
                .codecRegistry(Template.defaultRegistry)
                .build()
        )

        if (!checkConnection(client)) { return }

        val amendedBatchSize = min(batchSize, number.toInt())
        val amendedRate = tps?.div(amendedBatchSize)?.let { Rate.of(it) } ?: Rate.MAX

        val loadStage = MultiExecutionStage(
            name = "load stage",
            workers = workers,
            workloads = listOf(
                MultiExecutionWorkload(
                    name = if (amendedBatchSize == 1) "insertOne" else "insertMany",
                    rate = amendedRate,
                    count = number / amendedBatchSize,
                    executor = InsertManyExecutor(
                        client = client,
                        db = namespaceOptions.database,
                        collection = namespaceOptions.collection,
                        number = amendedBatchSize,
                        ordered = ordered,
                        template = template,
                    ),
                )
            ),
        )

        val stages = if (drop) {
            arrayOf(
                SingleExecutionStage(
                    name = "drop ${namespaceOptions.collection} collection",
                    executor = DropExecutor(
                        client = client,
                        db = namespaceOptions.database,
                        collection = namespaceOptions.collection
                    )
                ), loadStage
            )
        } else {
            arrayOf(loadStage)
        }

        println("Starting load...")

        val duration = runBlocking {
            measureTime {
                executeStages(*stages)
                    .format(ReportFormatter.create(ReportFormat.TEXT))
                    .collect {
                        print(it)
                    }
            }
        }

        println("\nCompleted in $duration (${(number / duration.toDouble(DurationUnit.SECONDS)).roundToInt()} inserts/s)")

    }

}
