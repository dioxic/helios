package uk.dioxic.helios.generate

import kotlinx.serialization.Serializable
import org.bson.BsonDocument
import org.bson.BsonDocumentWrapper
import org.bson.BsonValue
import org.bson.Document
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson
import uk.dioxic.helios.generate.serialization.TemplateSerializer

@Serializable(TemplateSerializer::class)
class Template(val execution: Document, val definition: BsonValue? = null): Bson {

    companion object {
        val EMPTY = Template(Document(), BsonDocument())
    }

    override fun <TDocument : Any> toBsonDocument(
        documentClass: Class<TDocument>,
        codecRegistry: CodecRegistry
    ): BsonDocument {
        return BsonDocumentWrapper(execution, codecRegistry[Document::class.java])
    }

}

