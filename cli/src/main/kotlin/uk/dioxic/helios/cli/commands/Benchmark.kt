package uk.dioxic.helios.cli.commands

import arrow.fx.coroutines.resourceScope
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.mongodb.MongoClientSettings
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import uk.dioxic.helios.cli.checkConnection
import uk.dioxic.helios.cli.options.AuthOptions
import uk.dioxic.helios.cli.options.ConnectionOptions
import uk.dioxic.helios.cli.options.applyAuthOptions
import uk.dioxic.helios.cli.options.applyConnectionOptions
import uk.dioxic.helios.execute.execute
import uk.dioxic.helios.execute.format.ReportFormat
import uk.dioxic.helios.execute.format.ReportFormatter
import uk.dioxic.helios.execute.format.format
import uk.dioxic.helios.execute.resources.ResourceRegistry
import uk.dioxic.helios.execute.resources.mongoClient
import uk.dioxic.helios.generate.Template
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import uk.dioxic.helios.execute.model.Benchmark as ExBenchmark

class Benchmark : CliktCommand(help = "Execute Benchmark") {
    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    private val authOptions by AuthOptions().cooccurring()
    private val connOptions by ConnectionOptions()
    private val workers by option(help = "number of workers").int().default(4)
    private val benchmark by argument(name = "benchmark").file(
        mustBeReadable = true,
        mustExist = true,
        canBeDir = false
    ).convert { Json.decodeFromString<ExBenchmark>(it.readText()) }

    @OptIn(ExperimentalTime::class)
    override fun run() {

        val mcs = MongoClientSettings.builder()
            .applyAuthOptions(authOptions)
            .applyConnectionOptions(connOptions)
            .codecRegistry(Template.defaultRegistry)
            .build()

        println("Starting benchmark...")


        val duration = runBlocking {
            resourceScope {
                val client = mongoClient(mcs)
                val registry = ResourceRegistry(client)

                if (!checkConnection(client)) {
                    error("Can't connect!")
                }
                measureTime {
                    benchmark.execute(registry, workers)
                        .format(ReportFormatter.create(ReportFormat.TEXT))
                        .collect {
                            println(it)
                        }
                }
            }
        }

        println("\nCompleted benchmark in $duration")

    }

}
