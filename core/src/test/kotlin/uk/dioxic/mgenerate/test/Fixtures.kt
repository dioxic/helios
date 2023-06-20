package uk.dioxic.mgenerate.test

import kotlinx.serialization.json.put
import uk.dioxic.mgenerate.buildTemplate
import uk.dioxic.mgenerate.put
import uk.dioxic.mgenerate.worker.model.InsertOneExecutor
import uk.dioxic.mgenerate.worker.model.MessageExecutor
import java.time.LocalDateTime
import java.util.*

val defaultTemplate = buildTemplate {
    put("name", "\$name")
    put("date", LocalDateTime.now())
    put("uuid", UUID.randomUUID())
    put("long", Long.MAX_VALUE)
}

val defaultExecutor = MessageExecutor("hello world!")

val defaultMongoExecutor = InsertOneExecutor(
    database = "myDB",
    collection = "myCollection",
    template = defaultTemplate
)