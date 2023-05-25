package uk.dioxic.mgenerate.codecs

import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.*
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import org.bson.codecs.jsr310.Jsr310CodecProvider
import uk.dioxic.mgenerate.OperatorTransformer
import uk.dioxic.mgenerate.Template

class TemplateDocumentCodec(
    registry: CodecRegistry = CodecRegistries.fromProviders(
        listOf(
            ValueCodecProvider(), Jsr310CodecProvider(),
            CollectionCodecProvider(), IterableCodecProvider(), OperatorExecutionCodecProvider(),
            BsonValueCodecProvider(), DocumentCodecProvider(OperatorTransformer()), MapCodecProvider()
        )
    ),
    bsonTypeClassMap: BsonTypeClassMap = BsonTypeClassMap()
) : Codec<Template> {

    private val documentCodec: DocumentCodec

    init {
        documentCodec = DocumentCodec(registry, bsonTypeClassMap, OperatorTransformer())
    }

    override fun encode(writer: BsonWriter, value: Template, encoderContext: EncoderContext) =
        documentCodec.encode(writer, value, encoderContext)

    override fun getEncoderClass(): Class<Template> =
        Template::class.java

    override fun decode(reader: BsonReader, decoderContext: DecoderContext) =
        Template(documentCodec.decode(reader, decoderContext))

}