package uk.dioxic.helios.execute.resources

import arrow.fx.coroutines.ResourceScope
import com.mongodb.MongoClientSettings
import com.mongodb.client.ClientSession
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import uk.dioxic.helios.execute.mongodb.cached
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

suspend fun ResourceScope.mongoClient(settings: MongoClientSettings): MongoClient =
    install({ MongoClients.create(settings).cached() }) { client, _ -> client.close() }

suspend fun ResourceScope.mongoSession(client: MongoClient): ClientSession =
    install({ client.startSession() }) { session, _ -> session.close() }

//suspend fun ResourceScope.fileWriter(f: File): Writer =
//    install({ f.bufferedWriter() }) { writer, _ -> writer.close() }
//
//suspend fun ResourceScope.fileReader(f: File): Writer =
//    install({ f.bufferedWriter() }) { writer, _ -> writer.close() }

suspend fun ResourceScope.fileOutputStream(f: File): FileOutputStream =
    install({ f.outputStream() }) { stream, _ -> stream.close() }

suspend fun ResourceScope.fileInputStream(f: File): FileInputStream =
    install({ f.inputStream() }) { stream, _ -> stream.close() }

suspend fun ResourceScope.fileSink(f: String): BufferedSink =
    install({ FileSystem.SYSTEM.sink(f.toPath()).buffer() }) { source, _ -> source.close() }

suspend fun ResourceScope.fileAppendingSink(f: String): BufferedSink =
    install({ FileSystem.SYSTEM.appendingSink(f.toPath()).buffer() }) { source, _ -> source.close() }

suspend fun ResourceScope.fileSource(f: String): BufferedSource =
    install({ FileSystem.SYSTEM.source(f.toPath()).buffer() }) { source, _ -> source.close() }

suspend fun ResourceScope.resource(resource: String): BufferedSource =
    install({ FileSystem.RESOURCES.source(resource.toPath()).buffer()}) { source, _ -> source.close() }