package uk.dioxic.helios.cli.commands

import arrow.fx.coroutines.resourceScope
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.mongodb.MongoClientSettings
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.bson.Bson
import kotlinx.serialization.decodeFromString
import uk.dioxic.helios.cli.checkConnection
import uk.dioxic.helios.cli.options.AuthOptions
import uk.dioxic.helios.cli.options.ConnectionOptions
import uk.dioxic.helios.cli.options.applyAuthOptions
import uk.dioxic.helios.cli.options.applyConnectionOptions
import uk.dioxic.helios.execute.execute
import uk.dioxic.helios.execute.format.ReportFormat
import uk.dioxic.helios.execute.format.ReportFormatter
import uk.dioxic.helios.execute.format.format
import uk.dioxic.helios.execute.format.toFormatString
import uk.dioxic.helios.execute.resources.ResourceRegistry
import uk.dioxic.helios.execute.resources.mongoClient
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import uk.dioxic.helios.execute.model.Benchmark as ExBenchmark
import uk.dioxic.helios.generate.codecs.DocumentCodec as HeliosDocumentCodec

class Benchmark : CliktCommand(help = "Execute Benchmark") {
    init {
        context { helpFormatter = { ctx -> MordantHelpFormatter(ctx, showDefaultValues = true)} }
    }

    private val authOptions by AuthOptions().cooccurring()
    private val connOptions by ConnectionOptions()
    private val concurrency by option("-c", "--concurrency", help = "number of concurrent operations").int().default(4)
    private val outputFormat by option("-f", "--format", help = "output format")
        .enum<ReportFormat>().default(ReportFormat.TEXT)
    private val benchmark by argument(name = "file").file(
        mustBeReadable = true,
        mustExist = true,
        canBeDir = false
    ).convert { Bson.decodeFromString<ExBenchmark>(it.readText()) }

    @OptIn(ExperimentalTime::class)
    override fun run() {

        val mcs = MongoClientSettings.builder()
            .applyAuthOptions(authOptions)
            .applyConnectionOptions(connOptions)
            .codecRegistry(HeliosDocumentCodec.defaultRegistry)
            .build()


        runBlocking {
            resourceScope {
                val client = mongoClient(mcs)
                val registry = ResourceRegistry(client)

                if (checkConnection(client)) {
                    println("Starting benchmark...")
                    val duration = measureTime {
                        benchmark.execute(registry, concurrency)
                            .format(ReportFormatter.create(outputFormat))
                            .collect {
                                println(it)
                            }
                    }
                    println("\nCompleted benchmark in ${duration.toFormatString()}")
                }
            }
        }


    }

}
