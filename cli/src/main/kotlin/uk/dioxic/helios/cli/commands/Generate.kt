package uk.dioxic.helios.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.types.defaultStdout
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.outputStream
import kotlinx.serialization.bson.Bson
import kotlinx.serialization.decodeFromString
import uk.dioxic.helios.cli.enums.OutputType
import uk.dioxic.helios.cli.extensions.writeJson
import uk.dioxic.helios.generate.Template
import uk.dioxic.helios.generate.codecs.DocumentCodec as HeliosDocumentCodec

class Generate : CliktCommand(help = "Generate data and output to a file or stdout") {
    init {
        context { helpFormatter = { ctx -> MordantHelpFormatter(ctx, showDefaultValues = true)} }
    }

    private val number by option("-n", "--number", help = "number of documents to generate").int().default(1)
    private val outputStream by option("-o", "--output", help = "output file").outputStream().defaultStdout()
    private val outputType by option(help = "output type").switch(
        "--pretty" to OutputType.PRETTY,
        "--jsonArray" to OutputType.ARRAY
    ).default(OutputType.PRETTY)
    private val template by argument(name = "template").file(
        mustBeReadable = true,
        mustExist = true,
        canBeDir = false
    ).convert { Bson.decodeFromString<Template>(it.readText()) }

    override fun run() {
        outputStream.bufferedWriter().use {
            val seq = generateSequence { template }
                .take(number)

            it.writeJson(seq, HeliosDocumentCodec(), outputType.jsonWriterSettings(), outputType.isArray())
        }

    }
}