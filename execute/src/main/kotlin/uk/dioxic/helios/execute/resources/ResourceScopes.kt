package uk.dioxic.helios.execute.resources

import com.mongodb.MongoClientSettings
import com.mongodb.client.ClientSession
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import jdk.incubator.foreign.ResourceScope
import okio.*
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import uk.dioxic.helios.execute.mongodb.cached
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

suspend fun ResourceScope.mongoClient(settings: MongoClientSettings): MongoClient =
    install({ MongoClients.create(settings).cached() }) { client, _ -> client.close() }

suspend fun ResourceScope.mongoSession(client: MongoClient): ClientSession =
    install({ client.startSession() }) { session, _ -> session.close() }

suspend fun ResourceScope.fileOutputStream(f: File): FileOutputStream =
    install({ f.outputStream() }) { stream, _ -> stream.close() }

suspend fun ResourceScope.fileInputStream(f: File): FileInputStream =
    install({ f.inputStream() }) { stream, _ -> stream.close() }

suspend fun ResourceScope.fileSink(fs: FileSystem, f: String): BufferedSink =
    fileSink(fs, f.toPath())

suspend fun ResourceScope.fileSink(fs: FileSystem, f: File): BufferedSink =
    fileSink(fs, f.toOkioPath())

suspend fun ResourceScope.fileSink(fs: FileSystem, path: Path): BufferedSink =
    install({ fs.sink(path).buffer() }) { source, _ -> source.close() }

suspend fun ResourceScope.fileAppendingSink(fs: FileSystem, f: String): BufferedSink =
    fileAppendingSink(fs, f.toPath())

suspend fun ResourceScope.fileAppendingSink(fs: FileSystem, f: File): BufferedSink =
    fileAppendingSink(fs, f.toOkioPath())

suspend fun ResourceScope.fileAppendingSink(fs: FileSystem, path: Path): BufferedSink =
    install({ fs.appendingSink(path).buffer() }) { source, _ -> source.close() }

suspend fun ResourceScope.fileSource(fs: FileSystem, f: String): BufferedSource =
    fileSource(fs, f.toPath())

suspend fun ResourceScope.fileSource(fs: FileSystem, f: File): BufferedSource =
    fileSource(fs, f.toOkioPath())

suspend fun ResourceScope.fileSource(fs: FileSystem, path: Path): BufferedSource =
    install({ fs.source(path).buffer() }) { source, _ -> source.close() }

suspend fun ResourceScope.resource(resource: String): BufferedSource =
    install({ FileSystem.RESOURCES.source(resource.toPath()).buffer() }) { source, _ -> source.close() }