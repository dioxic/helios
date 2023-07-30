package uk.dioxic.helios.generate

import kotlinx.serialization.Serializable
import org.bson.BsonDocument
import org.bson.BsonValue
import org.bson.Document
import uk.dioxic.helios.generate.serialization.TemplateDefinitionSerializer

@Serializable(TemplateDefinitionSerializer::class)
class Template(execution: Document, val definition: BsonValue? = null): Document(execution) {

    companion object {
        val EMPTY = Template(Document(), BsonDocument())
    }

}

