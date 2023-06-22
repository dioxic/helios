package uk.dioxic.mgenerate.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.mongodb.MongoException
import com.mongodb.client.MongoClient
import org.bson.Document
import uk.dioxic.mgenerate.cli.commands.Generate
import uk.dioxic.mgenerate.cli.commands.Load

class Cli : CliktCommand() {
    override fun run() = Unit
}

fun checkConnection(client: MongoClient): Boolean =
    try {
        println("Checking connection...")
        client.getDatabase("test").runCommand(Document("ping", 1))
        true
    } catch (e: MongoException) {
        client.clusterDescription.srvResolutionException?.let {
            println(it.message)
            return false
        }
        val serverAddresses = client.clusterDescription.serverDescriptions.map { it.address }
        println("Failed to connect to $serverAddresses")
        false
    }

fun main(args: Array<String>) = Cli()
    .subcommands(Generate(), Load())
    .main(args)