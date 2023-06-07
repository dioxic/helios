package uk.dioxic.mgenerate.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import uk.dioxic.mgenerate.cli.commands.Generate

class Cli : CliktCommand() {
    override fun run() = Unit
}

fun main(args: Array<String>) = Cli()
    .subcommands(Generate())
    .main(args)